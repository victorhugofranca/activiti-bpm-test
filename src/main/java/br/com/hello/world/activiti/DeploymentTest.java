package br.com.hello.world.activiti;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.activiti.engine.HistoryService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricVariableInstance;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.variable.EntityManagerSession;
import org.activiti.engine.impl.variable.EntityManagerSessionFactory;
import org.activiti.engine.task.Task;

import br.com.hello.world.activiti.entity.Parte;

public class DeploymentTest {

	private ProcessEngineConfiguration processEngineConfiguration;

	private ProcessEngine processEngine;

	private RepositoryService repositoryService;
	private RuntimeService runtimeService;
	private HistoryService historyService;
	private TaskService taskService;

	public static void main(String[] args) {

		// DbSchemaCreate.main(args);
		DeploymentTest deploymentTest = new DeploymentTest();

		deploymentTest.initActivitiServices();

		try {

			deploymentTest.deployProcess();

			String procId = deploymentTest.initProc();
			deploymentTest.runTaskOne(procId, "true");
			deploymentTest.runTaskOne(procId, "false");
			deploymentTest.runTaskTwo(procId);

			deploymentTest.testLoadVarHist(procId);
		} finally {
			deploymentTest.closeActivitiEngige();
			deploymentTest.deleteEntities();
		}

	}

	private void initActivitiServices() {
		processEngineConfiguration = ProcessEngineConfiguration
				.createProcessEngineConfigurationFromResourceDefault();

		processEngine = processEngineConfiguration.buildProcessEngine();

		// Get Activiti services
		repositoryService = processEngine.getRepositoryService();
		runtimeService = processEngine.getRuntimeService();
		historyService = processEngine.getHistoryService();
		taskService = processEngine.getTaskService();
	}

	private EntityManager getEntityManager() {
		EntityManagerSessionFactory entityManagerSessionFactory = (EntityManagerSessionFactory) ((ProcessEngineConfigurationImpl) processEngineConfiguration)
				.getSessionFactories().get(EntityManagerSession.class);

		EntityManagerFactory entityManagerFactory = entityManagerSessionFactory
				.getEntityManagerFactory();
		return entityManagerFactory.createEntityManager();
	}

	private void closeActivitiEngige() {
		processEngine.close();
	}

	private void deployProcess() {
		// Deploy the process definition
		repositoryService.createDeployment()
				.addClasspathResource("process.bpmn20.xml").deploy();
	}

	private String initProc() {

		return runtimeService.startProcessInstanceByKey("financialReport")
				.getId();

	}

	private void runTaskOne(String procId, String resend) {

		// Get the first task
		Task task = taskService.createTaskQuery().singleResult();
		System.out
				.println("Following task is available for accountancy group: "
						+ task.getName());
		// claim it
		taskService.claim(task.getId(), "fozzie");

		Map<String, Object> map = new LinkedHashMap<String, Object>();

		Parte parte = loadParteEntity(1);

		map.put("parte", parte);
		map.put("resendRequest", resend);

		// Complete the task
		taskService.setVariablesLocal(task.getId(), map);
		taskService.complete(task.getId(), map);

		System.out.println("Number of tasks for fozzie: "
				+ taskService.createTaskQuery().taskAssignee("fozzie").count());
	}

	private Parte loadParteEntity(Integer id) {
		EntityManager em = getEntityManager();

		Parte parte = em.find(Parte.class, id);

		if (parte == null) {
			parte = new Parte();
			parte.setId(id);
			parte.setDescricao("Victor França");

			em.getTransaction().begin();
			em.persist(parte);
			em.flush();
			em.getTransaction().commit();
		}

		em.close();
		return parte;
	}

	private void deleteEntities() {
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		em.createQuery("delete Parte").executeUpdate();
		em.getTransaction().commit();
		em.close();
	}

	private void runTaskTwo(String procId) {
		// Retrieve and claim the second task
		Task task = taskService.createTaskQuery().processInstanceId(procId)
				.singleResult();
		System.out
				.println("Following task is available for accountancy group: "
						+ task.getName());
		taskService.claim(task.getId(), "kermit");

		taskService.complete(task.getId());

	}

	private void testLoadVarHist(String processInstanceId) {

		List<HistoricVariableInstance> vars = historyService
				.createHistoricVariableInstanceQuery()
				.processInstanceId(processInstanceId).list();

		for (Iterator<HistoricVariableInstance> iterator = vars.iterator(); iterator
				.hasNext();) {
			HistoricVariableInstance historicVariableInstance = iterator.next();
			Object var = historicVariableInstance.getValue();
			if (var != null) {
				System.out.println("Var ID: "
						+ historicVariableInstance.getId());
				System.out.println("Var Name: "
						+ historicVariableInstance.getVariableName());
				System.out.println("Var Type: "
						+ historicVariableInstance.getVariableTypeName());

				if (var instanceof Parte) {
					System.out.println("Parte ID: " + ((Parte) var).getId());
					System.out.println("Parte Descrição: "
							+ ((Parte) var).getDescricao());
				} else {
					System.out.println("Var Value: " + var);
				}

				System.out.println("");
			}

		}

	}
}
