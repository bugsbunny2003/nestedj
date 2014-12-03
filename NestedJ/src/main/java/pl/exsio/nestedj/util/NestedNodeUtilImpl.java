/* 
 * The MIT License
 *
 * Copyright 2014 exsio.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package pl.exsio.nestedj.util;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.Id;
import pl.exsio.nestedj.NestedNodeUtil;
import pl.exsio.nestedj.annotation.LeftColumn;
import pl.exsio.nestedj.annotation.LevelColumn;
import pl.exsio.nestedj.annotation.ParentColumn;
import pl.exsio.nestedj.annotation.RightColumn;
import pl.exsio.nestedj.config.NestedNodeConfig;
import pl.exsio.nestedj.config.NestedNodeConfigImpl;
import pl.exsio.nestedj.model.NestedNode;

/**
 *
 * @author exsio
 * @param <T>
 */
public class NestedNodeUtilImpl<T extends NestedNode> implements NestedNodeUtil<T> {

    protected Map<Class<? extends NestedNode>, NestedNodeConfig> configs;

    public NestedNodeUtilImpl() {
        this.configs = new HashMap<Class<? extends NestedNode>, NestedNodeConfig>();
    }

    @Override
    public boolean isNodeNew(T node) {
        return (node.getLeft() == null && node.getRight() == null);
    }

    @Override
    public NestedNodeConfig getNodeConfig(Class<? extends NestedNode> nodeClass) {
        if (!this.configs.containsKey(nodeClass)) {
            NestedNodeConfigImpl config = new NestedNodeConfigImpl();

            Entity entity = nodeClass.getAnnotation(Entity.class);
            String name = entity.name();
            config.setEntityName((name != null && name.length() > 0) ? name : nodeClass.getSimpleName());
            config = this.getConfigForClass(nodeClass, config);
            this.configs.put(nodeClass, config);
        }

        return this.configs.get(nodeClass);
    }

    private NestedNodeConfigImpl getConfigForClass(Class<? extends NestedNode> nodeClass, NestedNodeConfigImpl config) {

        if(!nodeClass.getCanonicalName().equals(Object.class.getCanonicalName())) {
            for (Field field : nodeClass.getDeclaredFields()) {
                if (field.getAnnotation(LeftColumn.class) != null && config.getLeftFieldName() == null) {
                    config.setLeftFieldName(field.getName());
                } else if (field.getAnnotation(RightColumn.class) != null && config.getRightFieldName() == null) {
                    config.setRightFieldName(field.getName());
                } else if (field.getAnnotation(LevelColumn.class) != null && config.getLevelFieldName() == null) {
                    config.setLevelFieldName(field.getName());
                } else if (field.getAnnotation(ParentColumn.class) != null && config.getParentFieldName() == null) {
                    config.setParentFieldName(field.getName());
                } else if (field.getAnnotation(Id.class) != null && config.getIdFieldName() == null) {
                    config.setIdFieldName(field.getName());
                }
            }
            config = this.getConfigForClass((Class<? extends NestedNode>) nodeClass.getSuperclass(), config);
        }
        return config;
    }

}