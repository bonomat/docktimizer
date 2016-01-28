package au.csiro.data61.docktimizer.models;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 */
@XmlRootElement(name = "DockerEnvironmentVariable")
@Entity
@Table(name = "DockerEnvironmentVariable")
public class DockerEnvironmentVariable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String fieldAndValue;

    public DockerEnvironmentVariable(String fieldAndValue) {
        this.fieldAndValue = fieldAndValue;
    }

    public DockerEnvironmentVariable() {
    }

    public String getFieldAndValue() {
        return fieldAndValue;
    }

    public void setFieldAndValue(String fieldAndValue) {
        this.fieldAndValue = fieldAndValue;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
