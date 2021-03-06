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
package org.activiti.form.engine.impl.persistence.entity.data.impl;

import java.util.List;

import org.activiti.engine.impl.Page;
import org.activiti.form.api.FormInstance;
import org.activiti.form.engine.FormEngineConfiguration;
import org.activiti.form.engine.impl.FormInstanceQueryImpl;
import org.activiti.form.engine.impl.persistence.entity.FormInstanceEntity;
import org.activiti.form.engine.impl.persistence.entity.FormInstanceEntityImpl;
import org.activiti.form.engine.impl.persistence.entity.data.AbstractDataManager;
import org.activiti.form.engine.impl.persistence.entity.data.FormInstanceDataManager;

/**
 * @author Tijs Rademakers
 */
public class MybatisFormInstanceDataManager extends AbstractDataManager<FormInstanceEntity> implements FormInstanceDataManager {
  
  public MybatisFormInstanceDataManager(FormEngineConfiguration formEngineConfiguration) {
    super(formEngineConfiguration);
  }

  @Override
  public Class<? extends FormInstanceEntity> getManagedEntityClass() {
    return FormInstanceEntityImpl.class;
  }
  
  @Override
  public FormInstanceEntity create() {
    return new FormInstanceEntityImpl();
  }
  
  @Override
  public long findFormInstanceCountByQueryCriteria(FormInstanceQueryImpl formInstanceQuery) {
    return (Long) getDbSqlSession().selectOne("selectFormInstanceCountByQueryCriteria", formInstanceQuery);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<FormInstance> findFormInstancesByQueryCriteria(FormInstanceQueryImpl formInstanceQuery, Page page) {
    final String query = "selectFormInstancesByQueryCriteria";
    return getDbSqlSession().selectList(query, formInstanceQuery, page);
  }
}
