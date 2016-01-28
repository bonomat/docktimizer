package au.csiro.data61.docktimizer.controller;

import au.csiro.data61.docktimizer.helper.VMTypeComparator;
import au.csiro.data61.docktimizer.hibernate.EntityManagerHelper;
import au.csiro.data61.docktimizer.interfaces.DatabaseController;
import au.csiro.data61.docktimizer.models.*;
import au.csiro.data61.docktimizer.placement.DockerPlacementService;
import com.amazonaws.services.opsworks.model.EnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 */
public class MysqlDatabaseController implements DatabaseController {
    private static final Logger LOG = (Logger) LoggerFactory.getLogger(MysqlDatabaseController.class);

    private static MysqlDatabaseController instance;

    public static final String ONE_ALL = "1ALL";
    public static final String DEFAULT = "DEFAULT";
    public static final String ONE_EACH = "1EACH";

    //Chose which evaluation you are runnging...
    //    public static String BASELINE_TYPE = "DEFAULT";
    //    public static String BASELINE_TYPE = "1EACH";
    public static String BASELINE_TYPE = ONE_EACH;

    protected static Integer V = 3; // type = cores
    protected static Integer K = 4; // how many of each core
    protected static Integer D = 3; // how many docker types
    protected static Integer C = 4; // different configurations of each docker type
    private EntityManagerHelper entityManager;

    private SortedMap<VMType, List<VirtualMachine>> vmMap;
    private Map<DockerContainer, List<DockerContainer>> dockerMap;
    private Map<String, Integer> invocationMap;
    private List<DockerImage> imageList;
//    private static Session session;

    private MysqlDatabaseController() {
        entityManager = EntityManagerHelper.getInstance();
        switch (BASELINE_TYPE) {
            case ONE_EACH:
                V = 3;
                K = 12;
                D = 3;
                C = 4;
                return;
            case ONE_ALL:
                V = 3;
                K = 4;
                D = 3;
                C = 4;
                return;
            default:
                V = 3;
                K = 4;
                D = 3;
                C = 4;
                break;
        }
    }

    public static MysqlDatabaseController getInstance() {
        if (instance == null) {
            instance = new MysqlDatabaseController();
        }
        return instance;
    }

    @Override
    public synchronized void initializeAndUpdate(long tau_t) {

        entityManager.getEntityManager().getTransaction().begin();


        vmMap = new TreeMap<>(new VMTypeComparator());
        dockerMap = new HashMap<>();
        invocationMap = new HashMap<>();
        imageList = new ArrayList<>();


        //setup list of VMs
        List<VirtualMachine> virtualMachineList = updateVMMap();
        if (virtualMachineList.isEmpty()) {
            initializeVirtualMachines();
        }
        LOG.info("Loading images...");
        List<DockerImage> tmpImageList = entityManager.getEntityManager().createQuery("From DockerImage ").getResultList();
        if (tmpImageList.isEmpty()) {
            initializeDockerImages();
        } else {
            this.imageList = tmpImageList;
        }
        StringBuilder images = new StringBuilder("[");
        for (DockerImage dockerImage : imageList) {
            images.append(dockerImage.getAppId()).append(",");
        }

        images.append("]");
        LOG.info("Found images " + imageList.size() + " " + images.toString());


        //setup DockerContainers
        LOG.info("Loading docker container types...");
        List<DockerContainer> containerList = updateDockerMap();
        if (containerList.isEmpty()) {
            initializeDockerContainer();
        }
        Set<DockerContainer> dockerContainers = dockerMap.keySet();
        StringBuilder containers = new StringBuilder("[");
        for (DockerContainer container : dockerContainers) {
            List<DockerContainer> dockerContainers1 = dockerMap.get(container);
            for (DockerContainer dockerContainer : dockerContainers1) {
                containers.append(dockerContainer.getName()).append(",");
            }
        }
        containers.append("]");


        LOG.info("Found docker container types " + dockerContainers.size() + " " + containers.toString());


        LOG.info("Loading planned invocations ...");
        long startTime = tau_t - 60 * 1000;
        long endTime = tau_t + DockerPlacementService.SLEEP_TIME;
        List<PlannedInvocation> plannedInvocations = entityManager.getEntityManager().createQuery("FROM PlannedInvocation AS pi WHERE pi.done=:done")
                .setParameter("done", false)
                .getResultList();

        LOG.info("Found planned invocations ..." + plannedInvocations.size());
        for (PlannedInvocation plannedInvocation : plannedInvocations) {
            Integer amount = invocationMap.get(plannedInvocation.getAppId());

            if (amount == null) {
                amount = 0;
            }
            amount += plannedInvocation.getAmount();
            invocationMap.put(plannedInvocation.getAppId(), amount);
            LOG.info("Found planned invocations " + plannedInvocation.getAppId() + " amount: " +
                    plannedInvocation.getAmount());
        }
        entityManager.getEntityManager().getTransaction().commit();
    }

    private List<DockerContainer> updateDockerMap() {
        List<DockerContainer> containerList = entityManager.getEntityManager().createQuery("From DockerContainer ").getResultList();
        for (DockerContainer dockerContainer : containerList) {
            List<DockerContainer> tmp = dockerMap.get(dockerContainer);
            if (tmp == null) {
                tmp = new ArrayList<>();
            }

            tmp.add(dockerContainer);
            dockerMap.put(dockerContainer, tmp);
        }
        return containerList;
    }

    private List<VirtualMachine> updateVMMap() {

        vmMap = new TreeMap<>();

        List<VirtualMachine> virtualMachineList = entityManager.getEntityManager().createQuery("From VirtualMachine ")
                .getResultList();
        for (VirtualMachine next : virtualMachineList) {
            List<VirtualMachine> tmp = vmMap.get(next.getVmType());
            if (tmp == null) {
                tmp = new ArrayList<>();
            }
            switch (BASELINE_TYPE) {
                case ONE_EACH:
                    //only one VM per container, this means we only use dual cores
                    if (next.getVmType().cores != VMType.M1_SMALL.cores) {
                        continue;
                    }
                    break;
                default:
                    break;
            }

            if (!tmp.contains(next)) {
                tmp.add(next);
            }
            vmMap.put(next.getVmType(), tmp);
        }
        return virtualMachineList;
    }

    private void initializeDockerImages() {
        for (int c = 0; c < D; c++) {
            DockerImage dockerImage = parseByAppId("app" + c);
//            entityManager.getEntityManager().persist(dockerImage);
            imageList.add(dockerImage);
        }
    }

    public static DockerImage parseByImageName(String imageFullName) {
        if (imageFullName.contains("bonomat")) {
            DockerImage bonomat = new DockerImage("app" + 0, "bonomat/nodejs-hello-world", 8080, 3000, null);
            return bonomat;
        }
        if (imageFullName.contains("kaihofstetter")) {
            DockerImage kaihofstetter = new DockerImage("app" + 1, "gjong/apache-joomla", 8081, 80, null);
            return kaihofstetter;

        }
        if (imageFullName.contains("wordpress")) {
            DockerImage wordpress = new DockerImage("app" + 2, "wordpress:4.4", 8082, 80, null);
            wordpress.setSibl(parseByImageName("mysql"));
            return wordpress;
        }

        if (imageFullName.contains("gjong")) {
            DockerImage gjong = new DockerImage("app" + 3, "bonomat/zero-to-wordpress-sqlite", 8083, 80, null);
            return gjong;
        }
        if (imageFullName.contains("xhoussemx")) {
            DockerImage xhoussemx = new DockerImage("app" + 4, "bonomat/nodejs-hello-world", 8084, 3000, null);
            return xhoussemx;
        }

        if (imageFullName.contains("mysql")) {
            DockerImage mysql = new DockerImage("mysql", "mysql/mysql-server:5.5", 3306, 3306,
                    new DockerEnvironmentVariable("MYSQL_ROOT_PASSWORD=password"));
            return mysql;
        }
        return null;
    }


    public static DockerImage parseByAppId(String appId) {
        if (appId.contains("app0")) {
            return parseByImageName("bonomat");
        }
        if (appId.contains("app1")) {
            return parseByImageName("kaihofstetter");
        }
        if (appId.contains("app2")) {
            return parseByImageName("wordpress");
        }
        if (appId.contains("app3")) {
            return parseByImageName("gjong");
        }
        if (appId.contains("app4")) {
            return parseByImageName("xhoussemx");
        }
        return parseByImageName("bonomat");
    }

    private void initializeDockerContainer() {
        for (DockerImage dockerImage : imageList) {
            List<DockerContainer> dockerList = new ArrayList<>();
            for (int j = 0; j < C; j++) {
                DockerConfiguration configuration = null;
                switch (j) {
                    case 0:
                        configuration = DockerConfiguration.SINGLE_CORE;
                        break;
                    case 1:
                        configuration = DockerConfiguration.DUAL_CORE;
                        break;
                    case 2:
                        configuration = DockerConfiguration.QUAD_CORE;
                        break;
                    case 3:
                        configuration = DockerConfiguration.OCTA_CORE;
                        break;

                }

                DockerContainer container = new DockerContainer(dockerImage, configuration);

                if (dockerImage.getSibl() != null) {
                    DockerContainer siblingSettings = getSiblingSettings(dockerImage.getSibl());
                    container.setSibling(siblingSettings);
                    siblingSettings.setIsSibling(true);
                    entityManager.getEntityManager().persist(siblingSettings);
                }

                entityManager.getEntityManager().persist(container);
                dockerList.add(container);
            }
            dockerMap.put(dockerList.get(0), dockerList);
        }
    }

    private DockerContainer getSiblingSettings(DockerImage sibling) {
        DockerContainer container = new DockerContainer();

        if (sibling.getAppId().contains("mysql")) {
            container = new DockerContainer(sibling, DockerConfiguration.MICRO_CORE);
        }
        return container;
    }

    private void initializeVirtualMachines() {
        for (int type = 0; type < V; type++) {
            List<VirtualMachine> vmList = new ArrayList<>();
            VMType vmType = VMType.M1_MICRO;
            for (int j = 0; j < K; j++) {
                VirtualMachine vm;
                switch (type) {
                    case 0:
                        vmType = VMType.M1_SMALL;
                        break;
                    case 1:
                        vmType = VMType.M1_MEDIUM;
                        break;
                    case 2:
                        vmType = VMType.M1_LARGE;
                        break;
                    default:
                        vmType = VMType.M1_SMALL;
                        break;
                }
                vm = new VirtualMachine("k" + j + "v" + vmType.cores, vmType, j);
                vmList.add(vm);
                entityManager.getEntityManager().persist(vm);
            }
            vmMap.put(vmType, vmList);
        }
    }

    @Override
    public Map<DockerContainer, List<DockerContainer>> getDockerMap() {
        return dockerMap;
    }

    @Override
    public SortedMap<VMType, List<VirtualMachine>> getVmMap(boolean update) {
        if (update) {
            updateVMMap();
        }
        return vmMap;
    }

    @Override
    public int getInvocations(DockerContainer dockerContainerType) {
        Integer amount = invocationMap.get(dockerContainerType.getAppID());
        return amount == null ? 0 : amount;
    }

    @Override
    public void save(PlannedInvocation plannedInvocation) {
        entityManager.getEntityManager().getTransaction().begin();
        entityManager.getEntityManager().persist(plannedInvocation);
        entityManager.getEntityManager().getTransaction().commit();
    }

    @Override
    public synchronized void update(VirtualMachine virtualMachine) {
        entityManager.getEntityManager().getTransaction().begin();
        virtualMachine = entityManager.getEntityManager().merge(virtualMachine);
        entityManager.getEntityManager().getTransaction().commit();
    }

    @Override
    public List<PlannedInvocation> getInvocations() {
        List done = entityManager.getEntityManager().createQuery("FROM PlannedInvocation AS pi WHERE pi.done=:done")
                .setParameter("done", false)
                .getResultList();
        return done;
    }

    @Override
    public DockerContainer getDocker(String appID) {
        DockerContainer dockerContainer = (DockerContainer) entityManager.getEntityManager().createQuery(
                "FROM DockerContainer AS d WHERE d.dockerImage.appId=:appID")
                .setParameter("appID", appID).getSingleResult();
        return dockerContainer;
    }

    @Override
    public void close() {
        entityManager.getEntityManager().close();
    }


    @Override
    public void restart() {
        switch (BASELINE_TYPE) {
            case "1EACH":
                V = 1;
                K = 12;
                D = 3;
                C = 4;
                return;
            case "1ALL":
                V = 1;
                K = 4;
                D = 3;
                C = 4;
                return;
            default:
                V = 4;
                K = 4;
                D = 3;
                C = 4;
                break;
        }
    }
}
