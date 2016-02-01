package au.csiro.data61.docktimizer.controller;

import au.csiro.data61.docktimizer.AbstractTest;
import au.csiro.data61.docktimizer.models.DockerContainer;
import au.csiro.data61.docktimizer.models.VMType;
import au.csiro.data61.docktimizer.models.VirtualMachine;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MysqlDatabaseControllerTest extends AbstractTest {

    private static MysqlDatabaseController dbController;

    @BeforeClass
    public static void setUp() throws Exception {
        dbController = MysqlDatabaseController.getInstance();
        dbController.initializeAndUpdate(new Date().getTime());
    }

    @Test
    public void test01_testGetVmMap() throws Exception {
        SortedMap<VMType, List<VirtualMachine>> vmMap = dbController.getVmMap(false);
        assertThat(vmMap.size(), is(MysqlDatabaseController.V));

        for (VMType vmType : vmMap.keySet()) {
            List<VirtualMachine> containerList = vmMap.get(vmType);
            assertThat(containerList.size(), is(MysqlDatabaseController.K));
        }
    }

    @Test()
    public void test02_testGetDockerMap() throws Exception {
        Map<DockerContainer, List<DockerContainer>> dockerMap = dbController.getDockerMap();
        List<DockerContainer> realContainers = new ArrayList<>();
        for (DockerContainer dockerContainer : dockerMap.keySet()) { //only take the "real" apps, have "app" in the name
            for (DockerContainer container : dockerMap.get(dockerContainer)) {
                if (container.getName().contains("app")) {
                    realContainers.add(container);
                }
            }
        }
        assertThat(realContainers.size(), is(MysqlDatabaseController.D * MysqlDatabaseController.C));
        for (DockerContainer dockerContainer : dockerMap.keySet()) {
            List<DockerContainer> containerList = dockerMap.get(dockerContainer);
            if (dockerContainer.getName().contains("app")) {
                assertThat(containerList.size(), is(MysqlDatabaseController.C));
            }
        }
    }

    @Test
    public void test03_testGetVmMap_AndUpdate() throws Exception {
        SortedMap<VMType, List<VirtualMachine>> vmMap = dbController.getVmMap(false);
        assertThat(vmMap.size(), is(MysqlDatabaseController.V));

        Map<DockerContainer, List<DockerContainer>> dockerMap = dbController.getDockerMap();
        DockerContainer one = null;
        for (DockerContainer dockerContainer : dockerMap.keySet()) {
            List<DockerContainer> containerList = dockerMap.get(dockerContainer);
            one = containerList.get(0);
            break;
        }

        for (VMType vmType : vmMap.keySet()) {
            List<VirtualMachine> virtualMachines = vmMap.get(vmType);
            assertThat(virtualMachines.size(), is(MysqlDatabaseController.K));
            virtualMachines.get(0).addDockerContainer(one);
            dbController.update(virtualMachines.get(0));
        }


        dbController = MysqlDatabaseController.getInstance();
        dbController.initializeAndUpdate(new Date().getTime());
        vmMap = dbController.getVmMap(false);
        assertThat(vmMap.size(), is(MysqlDatabaseController.V));

        int deployedContainer = 0;
        for (VMType vmType : vmMap.keySet()) {
            List<VirtualMachine> virtualMachines = vmMap.get(vmType);
            assertThat(virtualMachines.size(), is(MysqlDatabaseController.K));
            for (VirtualMachine virtualMachine : virtualMachines) {
                deployedContainer += virtualMachine.getDeployedContainers().size();
            }
        }
        assertThat(deployedContainer, is(MysqlDatabaseController.V));
    }

    @Test
    public void test04_getDockerByAppId() {
        DockerContainer docker = dbController.getDocker(validDockerContainer.getAppID());
        assertThat(docker == null, is(false));
    }
}