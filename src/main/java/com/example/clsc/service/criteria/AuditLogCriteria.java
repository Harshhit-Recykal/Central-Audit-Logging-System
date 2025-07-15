package com.example.clsc.service.criteria;


import lombok.Data;

import java.io.Serializable;

@Data
public class AuditLogCriteria implements Serializable {

    private String entityName;
    private String entityId;
    private String action;
    private String changedBy;

}