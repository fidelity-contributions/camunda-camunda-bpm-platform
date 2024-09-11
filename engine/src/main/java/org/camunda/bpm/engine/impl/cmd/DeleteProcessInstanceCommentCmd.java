/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.camunda.bpm.engine.impl.cmd;

import static org.camunda.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import org.camunda.bpm.engine.BadUserRequestException;
import org.camunda.bpm.engine.history.UserOperationLogEntry;
import org.camunda.bpm.engine.impl.cfg.CommandChecker;
import org.camunda.bpm.engine.impl.interceptor.Command;
import org.camunda.bpm.engine.impl.interceptor.CommandContext;
import org.camunda.bpm.engine.impl.persistence.entity.CommentEntity;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.engine.impl.persistence.entity.PropertyChange;
import org.camunda.bpm.engine.impl.persistence.entity.TaskEntity;
import org.camunda.bpm.engine.task.Comment;

/**
 * Command to delete a comment by a given commentId and processInstanceId or to delete all comments
 * of a given processInstanceId
 */

public class DeleteProcessInstanceCommentCmd implements Command<Object>, Serializable {

  private static final long serialVersionUID = 1L;
  protected String commentId;
  protected String processInstanceId;

  public DeleteProcessInstanceCommentCmd(String processInstanceId, String commentId) {
    this.processInstanceId = processInstanceId;
    this.commentId = commentId;
  }

  public DeleteProcessInstanceCommentCmd(String processInstanceId) {
    this.processInstanceId = processInstanceId;
  }

  public Object execute(CommandContext commandContext) {
    if (processInstanceId == null && commentId == null) {
      throw new BadUserRequestException("Both process instance and comment ids are null");
    }

    ensureNotNull("processInstanceId", processInstanceId);
    ExecutionEntity processInstance = commandContext.getExecutionManager().findExecutionById(processInstanceId);
    ensureNotNull("No processInstance exists with processInstanceId: " + processInstanceId, "processInstance",
        processInstance);

    if (commentId != null && processInstanceId != null) {
      CommentEntity comment = commandContext.getCommentManager()
          .findCommentByProcessInstanceIdAndCommentId(processInstanceId, commentId);

      if (comment != null) {
        checkUpdateProcessInstanceById(processInstanceId, commandContext);
        commandContext.getDbEntityManager().delete(comment);
      }
    } else {
      List<Comment> comments = commandContext.getCommentManager().findCommentsByProcessInstanceId(processInstanceId);
      if (!comments.isEmpty()) {
        checkUpdateProcessInstanceById(processInstanceId, commandContext);
        commandContext.getCommentManager()
            .deleteCommentsByProcessInstanceIds(Collections.singletonList(processInstanceId));
      }
    }
    logOperation(processInstance, commandContext);
    return null;
  }

  private void logOperation(ExecutionEntity processInstance, CommandContext commandContext) {
    PropertyChange propertyChange = new PropertyChange("comment", null, null);
    commandContext.getOperationLogManager()
        .logCommentOperation(UserOperationLogEntry.OPERATION_TYPE_DELETE_COMMENT, processInstance, propertyChange);
  }

  protected void checkTaskWork(TaskEntity task, CommandContext commandContext) {
    for (CommandChecker checker : commandContext.getProcessEngineConfiguration().getCommandCheckers()) {
      checker.checkTaskWork(task);
    }
  }

  protected void checkUpdateProcessInstanceById(String processInstanceId, CommandContext commandContext) {
    for (CommandChecker checker : commandContext.getProcessEngineConfiguration().getCommandCheckers()) {
      checker.checkUpdateProcessInstanceById(processInstanceId);
    }
  }

}
