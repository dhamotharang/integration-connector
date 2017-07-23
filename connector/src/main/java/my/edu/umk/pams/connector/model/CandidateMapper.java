package my.edu.umk.pams.connector.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 */
@Component
public class CandidateMapper {
    private static final Logger LOG = LoggerFactory.getLogger(CandidateMapper.class);

    public CandidateMapper() {
    }

    public List<Candidate> process(List<Map<String, Object>> result) {
        LOG.info("candidate mapper");

        List<Candidate> codes = new ArrayList<Candidate>();
        for (Map<String, Object> map : result) {
            Candidate code = new Candidate();
            code.setMatricNo((String) map.get("matric_no"));
            code.setName((String) map.get("name"));
            codes.add(code);
        }
        return codes;
    }
}
