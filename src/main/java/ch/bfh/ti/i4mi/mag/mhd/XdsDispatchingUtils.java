package ch.bfh.ti.i4mi.mag.mhd;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.openehealth.ipf.commons.ihe.xds.core.ebxml.ebxml30.RetrieveDocumentSetRequestType;
import org.openehealth.ipf.commons.ihe.xds.core.ebxml.ebxml30.RetrieveDocumentSetResponseType;
import org.openehealth.ipf.commons.ihe.xds.core.requests.QueryRegistry;
import org.openehealth.ipf.commons.ihe.xds.core.requests.query.QueryType;
import org.openehealth.ipf.commons.ihe.xds.core.responses.Severity;
import org.openehealth.ipf.commons.ihe.xds.core.responses.Status;
import org.openehealth.ipf.commons.ihe.xds.core.stub.ebrs30.query.AdhocQueryRequest;
import org.openehealth.ipf.commons.ihe.xds.core.stub.ebrs30.query.AdhocQueryResponse;
import org.openehealth.ipf.commons.ihe.xds.core.stub.ebrs30.rim.RegistryObjectListType;
import org.openehealth.ipf.commons.ihe.xds.core.stub.ebrs30.rim.SlotListType;
import org.openehealth.ipf.commons.ihe.xds.core.stub.ebrs30.rs.RegistryErrorList;
import org.openehealth.ipf.commons.ihe.xds.core.stub.ebrs30.rs.RegistryResponseType;
import org.openehealth.ipf.commons.ihe.xds.core.transform.requests.QueryRegistryTransformer;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Dmytro Rud
 */
@Slf4j
public class XdsDispatchingUtils {

    private static final Set<String> QUERY_TYPES_WITH_TARGET_COMMUNITY_ID = Stream.of(
            QueryType.GET_DOCUMENTS,
            QueryType.GET_FOLDERS,
            QueryType.GET_ASSOCIATIONS,
            QueryType.GET_DOCUMENTS_AND_ASSOCIATIONS,
            QueryType.GET_SUBMISSION_SETS,
            QueryType.GET_SUBMISSION_SET_AND_CONTENTS,
            QueryType.GET_FOLDER_AND_CONTENTS,
            QueryType.GET_FOLDERS_FOR_DOCUMENT,
            QueryType.GET_RELATED_DOCUMENTS
    ).map(QueryType::getId).collect(Collectors.toSet());

    private static final QueryRegistryTransformer QUERY_REGISTRY_TRANSFORMER = new QueryRegistryTransformer();

    private final String homeCommunityId;

    @Getter
    private AdhocQueryRequest iti18RequestForDocumentRegistry;

    @Getter
    private AdhocQueryRequest iti18RequestForInitiatingGateway;

    @Getter
    private AdhocQueryResponse aggregatedIti18Response;

    @Getter
    private RetrieveDocumentSetResponseType aggregatedIti43Response;

    public XdsDispatchingUtils(String homeCommunityId) {
        Objects.requireNonNull(homeCommunityId, "EPR home community ID shall be provided");
        this.homeCommunityId = homeCommunityId.startsWith("urn:oid:") ? homeCommunityId : "urn:oid:" + homeCommunityId;
    }

    public void handleIti18Request(AdhocQueryRequest request) {
        if (QUERY_TYPES_WITH_TARGET_COMMUNITY_ID.contains(request.getAdhocQuery().getId())) {
            String targetHome = StringUtils.trimToNull(request.getAdhocQuery().getHome());
            if ((targetHome == null) || homeCommunityId.equalsIgnoreCase(targetHome)) {
                log.debug("ITI-18 request targets the local community, forward it to the Document Registry");
                iti18RequestForDocumentRegistry = request;
                iti18RequestForInitiatingGateway = null;
            } else {
                log.debug("ITI-18 request targets a foreign community, forward it to the Initiating Gateway");
                iti18RequestForDocumentRegistry = null;
                iti18RequestForInitiatingGateway = request;
            }
        } else {
            log.debug("ITI-18 request cannot target a community, forward it to both the Document Registry and the Initiating Gateway");
            iti18RequestForDocumentRegistry = request;
            iti18RequestForInitiatingGateway = request;
        }
    }

    public void handleIti18Request(QueryRegistry request) {
        handleIti18Request(QUERY_REGISTRY_TRANSFORMER.toEbXML(request).getInternal());
    }

    /**
     * @param response ITI-18 response from the local Document Registry
     * @return <code>true</code> whether the given response is a positive one and,
     * thus, whether the same request, whenever appropriate, shall be sent to the
     * Initiating Gateway
     */
    public boolean handleDocumentRegistryIti18Response(AdhocQueryResponse response) {
        boolean successful = Status.SUCCESS.getOpcode30().equals(response.getStatus());
        log.debug("ITI-18 response from the Document Registry denotes {}", (successful ? "success" : "failure"));
        aggregatedIti18Response = response;
        if (!successful) {
            log.debug("Cancel ITI-18 request to the Initiating Gateway");
            iti18RequestForInitiatingGateway = null;
        }
        return successful;
    }

    public void handleInitiatingGatewayIti18Response(AdhocQueryResponse response) {
        boolean successful = Status.SUCCESS.getOpcode30().equals(response.getStatus());
        log.debug("ITI-18 response from the Initiating Gateway denotes {}", (successful ? "success" : "failure"));
        if (aggregatedIti18Response == null) {
            log.debug("Return ITI-18 response from the Initiating Gateway to the client");
            aggregatedIti18Response = response;
        } else {
            log.debug("Aggregate ITI-18 responses from the Document Registry and the Initiating Gateway");
            if (!successful) {
                aggregatedIti18Response.setStatus(Status.PARTIAL_SUCCESS.getOpcode30());
            }
            boolean objectListsJoined = joinLists(response, aggregatedIti18Response,
                    AdhocQueryResponse::getRegistryObjectList,
                    AdhocQueryResponse::setRegistryObjectList,
                    RegistryObjectListType::getIdentifiable);
            boolean errorListsJoined = joinLists(response, aggregatedIti18Response,
                    AdhocQueryResponse::getRegistryErrorList,
                    AdhocQueryResponse::setRegistryErrorList,
                    RegistryErrorList::getRegistryError);
            boolean slotListsJoined = joinLists(response, aggregatedIti18Response,
                    AdhocQueryResponse::getResponseSlotList,
                    AdhocQueryResponse::setResponseSlotList,
                    SlotListType::getSlot);
            joinSeverities(errorListsJoined, response, aggregatedIti18Response);
        }
    }

    /**
     * @return array of two {@link RetrieveDocumentSetRequestType} elements: <ul>
     * <li>element 0 &mdash; request to be sent to the local Document Repository</li>
     * <li>element 1 &mdash; request to be sent to the Initiating Gateway</li>
     * </ul>
     */
    public RetrieveDocumentSetRequestType[] getIti43Requests(RetrieveDocumentSetRequestType request) {
        RetrieveDocumentSetRequestType localRequest = new RetrieveDocumentSetRequestType();
        RetrieveDocumentSetRequestType remoteRequest = new RetrieveDocumentSetRequestType();
        for (RetrieveDocumentSetRequestType.DocumentRequest reference : request.getDocumentRequest()) {
            String targetHomeCommunityId = reference.getHomeCommunityId();
            if (StringUtils.isBlank(targetHomeCommunityId) || homeCommunityId.equalsIgnoreCase(targetHomeCommunityId)) {
                reference.setHomeCommunityId(null);
                localRequest.getDocumentRequest().add(reference);
            } else {
                remoteRequest.getDocumentRequest().add(reference);
            }
        }
        log.debug("ITI-43 request from the client references {} local and {} remote documents",
                localRequest.getDocumentRequest().size(), remoteRequest.getDocumentRequest().size());
        if (localRequest.getDocumentRequest().isEmpty()) {
            localRequest = null;
        }
        if (remoteRequest.getDocumentRequest().isEmpty()) {
            remoteRequest = null;
        }
        return new RetrieveDocumentSetRequestType[]{localRequest, remoteRequest};
    }

    public boolean handleDocumentRepositoryIti43Response(RetrieveDocumentSetResponseType response) {
        aggregatedIti43Response = response;
        boolean successful = Status.SUCCESS.getOpcode30().equals(response.getRegistryResponse().getStatus());
        log.debug("ITI-43 response from the Document Repository denotes {}", (successful ? "success" : "failure"));
        return successful;
    }

    public void handleInitiatingGatewayIti43Response(RetrieveDocumentSetResponseType response) {
        boolean successful = Status.SUCCESS.getOpcode30().equals(response.getRegistryResponse().getStatus());
        log.debug("ITI-43 response from the Initiating Gateway denotes {}", (successful ? "success" : "failure"));

        if (aggregatedIti43Response == null) {
            aggregatedIti43Response = response;
            log.debug("Return ITI-43 response from the Initiating Gateway to the client");
        } else {
            log.debug("Aggregate ITI-43 responses from the Document Repository and the Initiating Gateway");
            if (!successful) {
                aggregatedIti43Response.getRegistryResponse().setStatus(Status.PARTIAL_SUCCESS.getOpcode30());
            }
            aggregatedIti43Response.getDocumentResponse().addAll(response.getDocumentResponse());
            boolean errorListsJoined = joinLists(response.getRegistryResponse(), aggregatedIti43Response.getRegistryResponse(),
                    RegistryResponseType::getRegistryErrorList,
                    RegistryResponseType::setRegistryErrorList,
                    RegistryErrorList::getRegistryError);
            boolean slotListsJoined = joinLists(response.getRegistryResponse(), aggregatedIti43Response.getRegistryResponse(),
                    RegistryResponseType::getResponseSlotList,
                    RegistryResponseType::setResponseSlotList,
                    SlotListType::getSlot);
            joinSeverities(errorListsJoined, response.getRegistryResponse(), aggregatedIti43Response.getRegistryResponse());
        }
    }

    private static boolean isError(String severity) {
        return Severity.ERROR.getOpcode30().equals(severity);
    }

    private static boolean isWarning(String severity) {
        return Severity.WARNING.getOpcode30().equals(severity);
    }

    /**
     * @return <code>false</code> if no join was actually performed as at most one of the objects has some elements; <code>true</code> otherwise.
     */
    private static <ObjectType extends RegistryResponseType, CollectionType, ElementType> boolean joinLists(
            ObjectType source,
            ObjectType target,
            Function<ObjectType, CollectionType> collectionGetter,
            BiConsumer<ObjectType, CollectionType> collectionSetter,
            Function<CollectionType, List<ElementType>> elementListGetter)
    {
        CollectionType sourceEnclosingObject = collectionGetter.apply(source);
        if (sourceEnclosingObject == null) {
            return false;
        }
        List<ElementType> sourceElements = elementListGetter.apply(sourceEnclosingObject);
        if ((sourceElements == null) || sourceElements.isEmpty()) {
            return false;
        }
        CollectionType targetEnclosingObject = collectionGetter.apply(target);
        if (targetEnclosingObject == null) {
            collectionSetter.accept(target, sourceEnclosingObject);
            return false;
        } else {
            List<ElementType> targetElements = elementListGetter.apply(targetEnclosingObject);
            targetElements.addAll(sourceElements);
            return true;
        }
    }

    private static void joinSeverities(boolean errorListsJoined, RegistryResponseType source, RegistryResponseType target) {
        if (errorListsJoined) {
            String sourceSeverity = source.getRegistryErrorList().getHighestSeverity();
            String targetSeverity = target.getRegistryErrorList().getHighestSeverity();
            if (isError(sourceSeverity) || (isWarning(sourceSeverity) && !isError(targetSeverity))) {
                target.getRegistryErrorList().setHighestSeverity(sourceSeverity);
            }
        }
    }

}
