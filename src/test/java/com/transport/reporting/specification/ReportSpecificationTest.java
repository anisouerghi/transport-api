package com.transport.reporting.specification;

import com.transport.reporting.dto.ReportCriteria;
import com.transport.reporting.entity.Report;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;

class ReportSpecificationTest {

    @Test
    void shouldBuildSpecificationFromCriteria() {
        ReportCriteria criteria = new ReportCriteria();
        criteria.setReference("SIG");

        Specification<Report> specification = ReportSpecification.fromCriteria(criteria);

        assertThat(specification).isNotNull();
    }
}
