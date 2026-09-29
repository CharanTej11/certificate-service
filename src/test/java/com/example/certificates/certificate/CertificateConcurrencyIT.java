package com.example.certificates.certificate;

import com.example.certificates.AbstractIntegrationTest;
import com.example.certificates.dto.request.IssueCertificateRequest;
import com.example.certificates.entity.Person;
import com.example.certificates.entity.Programme;
import com.example.certificates.entity.ProgrammeRule;
import com.example.certificates.enums.RuleType;
import com.example.certificates.repository.CertificateRepository;
import com.example.certificates.repository.PersonRepository;
import com.example.certificates.repository.ProgrammeRepository;
import com.example.certificates.repository.ProgrammeRuleRepository;
import com.example.certificates.service.CertificateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CertificateConcurrencyIT extends AbstractIntegrationTest {

    @Autowired CertificateService certificateService;
    @Autowired PersonRepository personRepository;
    @Autowired ProgrammeRepository programmeRepository;
    @Autowired ProgrammeRuleRepository ruleRepository;
    @Autowired CertificateRepository certificateRepository;

    @Test
    void concurrentRequestsForSameRouteProduceExactlyOneCertificate() throws Exception {
        Person person = personRepository.save(new Person(UUID.randomUUID(), "Race Runner"));
        Programme programme = programmeRepository.save(new Programme(UUID.randomUUID(), "Concurrency 101", null));
        ruleRepository.save(new ProgrammeRule(UUID.randomUUID(), programme.getId(), RuleType.ANYONE, null));

        int threads = 16;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger rejections = new AtomicInteger();

        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            tasks.add(() -> {
                startGate.await();
                try {
                    certificateService.issue(new IssueCertificateRequest(
                            person.getId(), programme.getId(), null, null, null));
                    successes.incrementAndGet();
                } catch (RuntimeException e) {
                    rejections.incrementAndGet();
                }
                return null;
            });
        }

        List<Future<Void>> futures = new ArrayList<>();
        for (Callable<Void> task : tasks) {
            futures.add(pool.submit(task));
        }
        startGate.countDown(); // fire all at once
        for (Future<Void> f : futures) {
            f.get();
        }
        pool.shutdown();

        long created = certificateRepository.findByPersonIdOrderByIssuedAtDesc(
                person.getId(), org.springframework.data.domain.PageRequest.of(0, 100)).getTotalElements();

        assertThat(created).isEqualTo(1);
        assertThat(successes.get()).isEqualTo(1);
        assertThat(rejections.get()).isEqualTo(threads - 1);
    }
}
