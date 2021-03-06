/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.app.service.runtime;

import javax.servlet.http.HttpServletResponse;

import org.activiti.app.model.runtime.CreateProcessInstanceRepresentation;
import org.activiti.app.model.runtime.ProcessInstanceRepresentation;
import org.activiti.app.security.SecurityUtils;
import org.activiti.app.service.api.UserCache;
import org.activiti.app.service.api.UserCache.CachedUser;
import org.activiti.app.service.exception.BadRequestException;
import org.activiti.app.service.exception.NotFoundException;
import org.activiti.content.api.ContentService;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.form.api.FormRepositoryService;
import org.activiti.form.api.FormService;
import org.activiti.form.model.FormModel;
import org.activiti.idm.api.User;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Tijs Rademakers
 */
@Service
@Transactional
public class ActivitiProcessInstanceService {

  private static final Logger logger = LoggerFactory.getLogger(ActivitiProcessInstanceService.class);

  @Autowired
  protected RepositoryService repositoryService;

  @Autowired
  protected HistoryService historyService;

  @Autowired
  protected RuntimeService runtimeService;
  
  @Autowired
  protected FormService formService;
  
  @Autowired
  protected FormRepositoryService formRepositoryService;

  @Autowired
  protected PermissionService permissionService;
  
  @Autowired
  protected ContentService contentService;
  
  @Autowired
  protected ActivitiCommentService commentService;

  @Autowired
  protected UserCache userCache;

  public ProcessInstanceRepresentation getProcessInstance(String processInstanceId, HttpServletResponse response) {

    HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();

    if (!permissionService.hasReadPermissionOnProcessInstance(SecurityUtils.getCurrentUserObject(), processInstance, processInstanceId)) {
      throw new NotFoundException("Process with id: " + processInstanceId + " does not exist or is not available for this user");
    }

    ProcessDefinitionEntity processDefinition = (ProcessDefinitionEntity) repositoryService.getProcessDefinition(processInstance.getProcessDefinitionId());

    User userRep = null;
    if (processInstance.getStartUserId() != null) {
      CachedUser user = userCache.getUser(processInstance.getStartUserId());
      if (user != null && user.getUser() != null) {
        userRep = user.getUser();
      }
    }

    ProcessInstanceRepresentation processInstanceResult = new ProcessInstanceRepresentation(processInstance, processDefinition, processDefinition.isGraphicalNotationDefined(), userRep);

    if (processDefinition.hasStartFormKey()) {
      FormModel formModel = runtimeService.getStartFormModel(processInstance.getProcessDefinitionId(), processInstance.getId());
      if (formModel != null) {
        processInstanceResult.setStartFormDefined(true);
      }
    }

    return processInstanceResult;
  }

  public FormModel getProcessInstanceStartForm(String processInstanceId, HttpServletResponse response) {

    HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();

    if (!permissionService.hasReadPermissionOnProcessInstance(SecurityUtils.getCurrentUserObject(), processInstance, processInstanceId)) {
      throw new NotFoundException("Process with id: " + processInstanceId + " does not exist or is not available for this user");
    }
    
    return runtimeService.getStartFormModel(processInstance.getProcessDefinitionId(), processInstance.getId());
  }
  
  public ProcessInstanceRepresentation startNewProcessInstance(CreateProcessInstanceRepresentation startRequest) {
    if (StringUtils.isEmpty(startRequest.getProcessDefinitionId())) {
      throw new BadRequestException("Process definition id is required");
    }
    
    ProcessDefinition processDefinition = repositoryService.getProcessDefinition(startRequest.getProcessDefinitionId());
    ProcessInstance processInstance = runtimeService.startProcessInstanceWithForm(startRequest.getProcessDefinitionId(), 
        startRequest.getOutcome(), startRequest.getValues(), startRequest.getName());
    
    User user = null;
    if (processInstance.getStartUserId() != null) {
      CachedUser cachedUser = userCache.getUser(processInstance.getStartUserId());
      if (cachedUser != null && cachedUser.getUser() != null) {
        user = cachedUser.getUser();
      }
    }
    return new ProcessInstanceRepresentation(processInstance, processDefinition, 
        ((ProcessDefinitionEntity) processDefinition).isGraphicalNotationDefined(), user);

  }

  public void deleteProcessInstance(String processInstanceId) {

    User currentUser = SecurityUtils.getCurrentUserObject();

    HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery()
        .processInstanceId(processInstanceId)
        .startedBy(String.valueOf(currentUser.getId())) // Permission
        .singleResult();

    if (processInstance == null) {
      throw new NotFoundException("Process with id: " + processInstanceId + " does not exist or is not started by this user");
    }

    if (processInstance.getEndTime() != null) {
      // Check if a hard delete of process instance is allowed
      if (!permissionService.canDeleteProcessInstance(currentUser, processInstance)) {
        throw new NotFoundException("Process with id: " + processInstanceId + " is already completed and can't be deleted");
      }

      // Delete all content related to the process instance
      contentService.deleteContentItemsByProcessInstanceId(processInstanceId);
      
      // Delete all comments on tasks and process instances
      commentService.deleteAllCommentsForProcessInstance(processInstanceId);
      
      // Finally, delete all history for this instance in the engine
      historyService.deleteHistoricProcessInstance(processInstanceId);

    } else {
      runtimeService.deleteProcessInstance(processInstanceId, "Cancelled by " + SecurityUtils.getCurrentUserId());
    }
  }
}
