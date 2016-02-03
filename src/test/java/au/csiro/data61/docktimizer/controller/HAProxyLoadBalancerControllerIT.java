package au.csiro.data61.docktimizer.controller;

import au.csiro.data61.docktimizer.AbstractTest;
import au.csiro.data61.docktimizer.helper.HAProxyConfigGenerator;
import au.csiro.data61.docktimizer.models.DockerConfiguration;
import au.csiro.data61.docktimizer.models.DockerContainer;
import au.csiro.data61.docktimizer.models.DockerImage;
import au.csiro.data61.docktimizer.models.VirtualMachine;
import au.csiro.data61.docktimizer.testClient.DockerPlacementServiceTest;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class HAProxyLoadBalancerControllerIT extends AbstractTest {

    private static final Logger LOG = (Logger) LoggerFactory.getLogger(DockerPlacementServiceTest.class);


    @Test
    public void testAddServer() throws Exception {
        List<DockerContainer> list = new ArrayList<>();
        DockerContainer container = new DockerContainer(new DockerImage("app0", "bonomat/nodejs-hello-world", 8083, 3000, null), DockerConfiguration.MICRO_CORE);
        DockerContainer container1 = new DockerContainer(new DockerImage("app1", "wordpress:4.4", 8080, 8080, null), DockerConfiguration.MICRO_CORE);
        list.add(container);
        list.add(container1);
//        list.add(joomlaDockerContainer);
//        list.add(validDockerContainer);
        validVirtualMachine.setDeplyoedContainers(list);
        List<VirtualMachine> vms = new ArrayList<>();
//        invalidVirtualMachine.setDeplyoedContainers(list);
        validVirtualMachine.setIp("128.131.172.116");
        vms.add(validVirtualMachine);
//        vms.add(invalidVirtualMachine);
//        validVirtualMachine.setIp("10.99.0.12");
        HAProxyLoadBalancerController controller = (HAProxyLoadBalancerController) HAProxyLoadBalancerController.getInstance();

        controller.initialize();
        controller.LOAD_BALANCER_UPDATE_URL = "http://localhost:3001/api/files";

        String postBody = HAProxyConfigGenerator.generateConfigFile(vms);
        LOG.info(postBody);
        Integer result = controller.updateHAProxyConfiguration(vms);
        assertTrue(result == 200);


    }


}