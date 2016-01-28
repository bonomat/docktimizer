package au.csiro.data61.docktimizer.models;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 */
@XmlRootElement(name = "DockerImage")
@Entity
@Table(name = "DockerImage")
public class DockerImage {

    @Transient
    private DockerImage sibl;
    public DockerImage() {
    }

    public DockerImage(String appId, String name, Integer externPort, Integer internPort, DockerEnvironmentVariable... dockerEnvironmentVariables) {
        this.appId = appId;
        this.name = name;
        this.internPort = internPort;
        this.externPort = externPort;
        if (dockerEnvironmentVariables != null) {
            this.dockerEnvironmentVariables = Arrays.asList(dockerEnvironmentVariables);
        } else {
            this.dockerEnvironmentVariables = new ArrayList<>();
        }
    }

    @Id
    private String appId;

    private String name;

    private Integer internPort;
    private Integer externPort;
    public String getFullName() {
        return name;
    }

    @OneToMany(cascade = CascadeType.PERSIST)
    private List<DockerEnvironmentVariable> dockerEnvironmentVariables;

    public Integer getInternPort() {
        return internPort;
    }

    public void setInternPort(Integer internPort) {
        this.internPort = internPort;
    }

    public Integer getExternPort() {
        return externPort;
    }

    public void setExternPort(Integer externPort) {
        this.externPort = externPort;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public void setSibl(DockerImage sibl) {
        this.sibl = sibl;
    }

    public DockerImage getSibl() {
        return sibl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<DockerEnvironmentVariable> getDockerEnvironmentVariables() {
        return dockerEnvironmentVariables;
    }

    public void setDockerEnvironmentVariables(List<DockerEnvironmentVariable> dockerEnvironmentVariables) {
        this.dockerEnvironmentVariables = dockerEnvironmentVariables;
    }
}
