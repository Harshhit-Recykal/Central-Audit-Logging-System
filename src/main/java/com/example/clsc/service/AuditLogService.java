package com.example.clsc.service;

import com.example.clsc.dto.AuditLogDto;
import com.example.clsc.entity.AuditLog;
import com.example.clsc.repository.AuditLogRepo;
import com.example.clsc.service.criteria.AuditLogCriteria;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuditLogService {

    private final AuditLogRepo auditLogRepo;

    private final ObjectMapper objectMapper;

    private final Logger logger = LoggerFactory.getLogger(AuditLogService.class);

    @Autowired
    public AuditLogService(AuditLogRepo auditLogRepo, ObjectMapper objectMapper) {
        this.auditLogRepo = auditLogRepo;
        this.objectMapper = objectMapper;
    }

    private Specification<AuditLog> createSpecification(AuditLogCriteria filter) {
        return (root, query, cb)->{
            Predicate predicate = cb.conjunction();
            if(filter.getEntityName()!=null) {
                predicate = cb.and(predicate, cb.like(cb.lower(root.get("entityName")), "%"+filter.getEntityName().toLowerCase()+"%"));
            }
            if(filter.getEntityId()!=null) {
                predicate = cb.and(predicate, cb.equal(root.get("entityId"), filter.getEntityId()));
            }
            if(filter.getAction()!=null) {
                predicate = cb.and(predicate, cb.equal(root.get("action"), filter.getAction()));
            }
            if(filter.getChangedBy()!=null) {
                predicate = cb.and(predicate, cb.equal(root.get("changedBy"), filter.getChangedBy()));
            }
            return predicate;
        };
    }

    public List<AuditLogDto> getAllLogs() {
        logger.info("getAllLogs");
        return auditLogRepo.findAll().
                stream()
                .map(auditLog ->{
                        AuditLogDto auditLogDto = new AuditLogDto();
                        auditLogDto = objectMapper.convertValue(auditLog, AuditLogDto.class);
                        return auditLogDto;
                })
                .collect(Collectors.toList());
    }

    public List<AuditLogDto> getLogsByFilter(AuditLogCriteria criteria, Pageable pageable) {
        logger.info("getLogsByFilter");
        Specification<AuditLog> specification = createSpecification(criteria);
        return auditLogRepo.findAll(specification, pageable)
                .get()
                .map(auditLog -> {
                    AuditLogDto auditLogDto = new AuditLogDto();
                    auditLogDto = objectMapper.convertValue(auditLog, AuditLogDto.class);
                    return auditLogDto;
                })
                .collect(Collectors.toList());
    }

}
