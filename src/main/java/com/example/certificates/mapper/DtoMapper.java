package com.example.certificates.mapper;

import com.example.certificates.dto.response.CertificateResponse;
import com.example.certificates.dto.response.DesignResponse;
import com.example.certificates.dto.response.PersonResponse;
import com.example.certificates.dto.response.ProgrammeResponse;
import com.example.certificates.dto.response.ProgrammeRuleResponse;
import com.example.certificates.entity.Certificate;
import com.example.certificates.entity.Design;
import com.example.certificates.entity.Person;
import com.example.certificates.entity.Programme;
import com.example.certificates.entity.ProgrammeRule;
import org.springframework.stereotype.Component;

/**
 * Entity -> response mapping. Certificate mapping reads only snapshot columns,
 * never live programme / design / person values.
 */
@Component
public class DtoMapper {

    public PersonResponse toPersonResponse(Person p) {
        return new PersonResponse(p.getId(), p.getName(), p.getCreatedAt());
    }

    public ProgrammeResponse toProgrammeResponse(Programme p) {
        return new ProgrammeResponse(p.getId(), p.getName(), p.getDescription(),
                p.isActive(), p.getCreatedAt(), p.getUpdatedAt());
    }

    public DesignResponse toDesignResponse(Design d) {
        return new DesignResponse(d.getId(), d.getName(), d.getContent(),
                d.isActive(), d.getCreatedAt(), d.getUpdatedAt());
    }

    public ProgrammeRuleResponse toRuleResponse(ProgrammeRule r) {
        return new ProgrammeRuleResponse(r.getId(), r.getProgrammeId(), r.getRuleType(), r.getThreshold());
    }

    public CertificateResponse toCertificateResponse(Certificate c) {
        return new CertificateResponse(
                c.getId(),
                c.getPublicCode(),
                c.getPersonNameSnapshot(),
                c.getProgrammeNameSnapshot(),
                c.getProgrammeDescriptionSnapshot(),
                c.getDesignNameSnapshot(),
                c.getDesignContentSnapshot(),
                c.getTrackNameSnapshot(),
                c.getRouteType(),
                c.getIssuedAt(),
                c.getStatus(),
                c.getCancellationReason(),
                c.getCancelledAt()
        );
    }
}
