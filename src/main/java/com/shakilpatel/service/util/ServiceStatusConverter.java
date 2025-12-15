package com.shakilpatel.service.util;


import com.shakilpatel.service.enums.ServiceStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.postgresql.util.PGobject;

import java.sql.SQLException;

@Converter(autoApply = true)
public class ServiceStatusConverter implements AttributeConverter<ServiceStatus, Object> {

    @Override
    public Object convertToDatabaseColumn(ServiceStatus attribute) {
        if (attribute == null) {
            return null;
        }

        return new PGobject(){
            {
                setType("service_status");
                try {
                    setValue(attribute.name());
                } catch (SQLException ex) {
                    System.getLogger(ServiceStatusConverter.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
                }
            }
        };
    }

    @Override
    public ServiceStatus convertToEntityAttribute(Object dbData) {
        return dbData == null ? null : ServiceStatus.valueOf(dbData.toString());
    }
}
