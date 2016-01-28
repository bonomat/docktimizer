package au.csiro.data61.docktimizer.placement;

import au.csiro.data61.docktimizer.controller.MysqlDatabaseController;
import au.csiro.data61.docktimizer.helper.MILPSolver;
import au.csiro.data61.docktimizer.models.*;
import au.csiro.data61.docktimizer.service.DockerPlacementRESTApi;
import net.sf.javailp.Linear;
import net.sf.javailp.OptType;
import net.sf.javailp.Problem;
import net.sf.javailp.Result;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 */
public class DockerPlacementTest {

    private DockerPlacement dockerPlacement;
    private MysqlDatabaseController databaseController;

    @Before
    public void setUp() throws Exception {
        databaseController = MysqlDatabaseController.getInstance();
        dockerPlacement = DockerPlacement.getInstance();
    }

    /**
     * Min 5 x + 10 y
     * Subject to
     * 3 x + 1 y >= 8
     * 4y >= 4
     * 2x <= 2
     */
    @Test
    public void test1() {
        MILPSolver placement = new MILPSolver();
        Problem problem = new Problem();

        Linear linear = new Linear();
        linear.add(5, "x");
        linear.add(10, "y");

        problem.setObjective(linear, OptType.MIN);

        linear = new Linear();
        linear.add(3, "x");
        linear.add(1, "y");

        problem.add(linear, ">=", 8);

        linear = new Linear();
        linear.add(4, "y");

        problem.add(linear, ">=", 4);

        linear = new Linear();
        linear.add(2, "x");

        problem.add(linear, "<=", 2);

        Result result = placement.solveProblem(problem);
        Number objective = result.getObjective();
        Number x = result.get("x");
        Number y = result.get("y");
        assertThat(objective.doubleValue(), is(55.0));
        assertThat(x.doubleValue(), is(1.0));
        assertThat(y.doubleValue(), is(5.0));
    }

    @Ignore("Run only manually, otherwise this will produce cost as VMs might be leased")
    @Test
    public void testSolvePlacement() throws Exception {

        long tau_t = new Date().getTime();
        DockerPlacementRESTApi dockerPlacementRESTApi = new DockerPlacementRESTApi();
        PlannedInvocations invocations = new PlannedInvocations();
        List<PlannedInvocation> ivocationList = new ArrayList<>();
        PlannedInvocation invocation = new PlannedInvocation("app2", 10, tau_t);
        ivocationList.add(invocation);
        invocations.setPlannedInvocations(ivocationList);
        dockerPlacementRESTApi.add(invocations);

        Result result = dockerPlacement.solvePlacement(tau_t);
        print(result, result.getObjective());

    }


    private void print(Result result, Number objective) {
        Map<DockerContainer, List<DockerContainer>> dockerMap = databaseController.getDockerMap();
        SortedMap<VMType, List<VirtualMachine>> vmMap = databaseController.getVmMap(false);

        Map<String, Number> xValues = new HashMap<>();
        Map<String, Number> yValues = new HashMap<>();
        Map<String, Number> betaValues = new HashMap<>();

        StringBuilder invocationOutput = new StringBuilder("");
        for (DockerContainer dockerContainerType : dockerMap.keySet()) {
            int invocations = databaseController.getInvocations(dockerContainerType);
            invocationOutput.append(dockerContainerType.getAppID()).append(": ")
                    .append(invocations).append("\n");
            for (DockerContainer dockerContainer : dockerMap.get(dockerContainerType)) {
                for (VMType vmType : vmMap.keySet()) {
                    for (VirtualMachine virtualMachine : vmMap.get(vmType)) {
                        String decisionVariableX = dockerPlacement.getDecisionVariableX(dockerContainer, virtualMachine);
                        xValues.put(decisionVariableX, result.get(decisionVariableX));

                        String decisionVariableY = dockerPlacement.getDecisionVariableY(virtualMachine);
                        yValues.put(decisionVariableY, result.get(decisionVariableY));

                        boolean running = virtualMachine.isRunning();
                        if (running) {
                            betaValues.put(dockerPlacement.getHelperVariableBeta(virtualMachine), 1);
                        }
                    }
                }
            }
        }


        StringBuilder y1Values = new StringBuilder("");
        StringBuilder y0Values = new StringBuilder("");
        StringBuilder x1Values = new StringBuilder("");
        StringBuilder beta1Values = new StringBuilder("");
        StringBuilder x0Values = new StringBuilder("");
        for (String variableName : yValues.keySet()) {
            Number value = yValues.get(variableName);
            if (value.intValue() == 0) {
                y0Values.append(variableName).append(";").append(value).append("\n");
            } else {
                y1Values.append(variableName).append(";").append(value).append("\n");
            }
        }
        for (String variableName : xValues.keySet()) {
            Number value = xValues.get(variableName);
            if (value.intValue() == 0) {
                x0Values.append(variableName).append(";").append(value).append("\n");
            } else {
                x1Values.append(variableName).append(";").append(value).append("\n");
            }
        }
        for (String variableName : betaValues.keySet()) {
            Number value = betaValues.get(variableName);
            if (value.intValue() == 0) {
                beta1Values.append(variableName).append(";").append(value).append("\n");
            } else {
                beta1Values.append(variableName).append(";").append(value).append("\n");
            }
        }


        System.out.println(""
                + "\nplanned invocations: \n" + invocationOutput.toString()
                + "\nBeta - 1 - values\n" + beta1Values.toString()
                + "\nobjective: " + objective
                + "\nY - 1 - values\n" + y1Values.toString()
                + "\nX - 1 - values\n" + x1Values.toString()
        );

    }
}
