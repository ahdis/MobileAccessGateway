package ch.bfh.ti.i4mi.mag.mpi.pdqm.iti119;

import ch.bfh.ti.i4mi.mag.config.props.MagMpiProps;
import ch.bfh.ti.i4mi.mag.mhd.SchemeMapper;
import ch.bfh.ti.i4mi.mag.mpi.pdqm.iti78.Iti78ResponseConverter;
import org.springframework.stereotype.Component;

@Component
public class Iti119ResponseConverter extends Iti78ResponseConverter {
    public Iti119ResponseConverter(final SchemeMapper schemeMapper,
                                   final MagMpiProps mpiProps) {
        super(schemeMapper, mpiProps);
    }
}
