/*
 * Copyright 2011 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.drools.ide.common.client.modeldriven.dt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.drools.ide.common.client.modeldriven.FieldNature;
import org.drools.ide.common.client.modeldriven.brl.ActionFieldList;
import org.drools.ide.common.client.modeldriven.brl.ActionFieldValue;
import org.drools.ide.common.client.modeldriven.brl.BaseSingleFieldConstraint;
import org.drools.ide.common.client.modeldriven.brl.CompositeFactPattern;
import org.drools.ide.common.client.modeldriven.brl.CompositeFieldConstraint;
import org.drools.ide.common.client.modeldriven.brl.DSLSentence;
import org.drools.ide.common.client.modeldriven.brl.FactPattern;
import org.drools.ide.common.client.modeldriven.brl.FieldConstraint;
import org.drools.ide.common.client.modeldriven.brl.FreeFormLine;
import org.drools.ide.common.client.modeldriven.brl.FromAccumulateCompositeFactPattern;
import org.drools.ide.common.client.modeldriven.brl.FromCollectCompositeFactPattern;
import org.drools.ide.common.client.modeldriven.brl.FromCompositeFactPattern;
import org.drools.ide.common.client.modeldriven.brl.IAction;
import org.drools.ide.common.client.modeldriven.brl.IFactPattern;
import org.drools.ide.common.client.modeldriven.brl.IPattern;
import org.drools.ide.common.client.modeldriven.brl.PortableObject;
import org.drools.ide.common.client.modeldriven.brl.RuleModel;
import org.drools.ide.common.client.modeldriven.brl.SingleFieldConstraint;

public class TemplateModel extends RuleModel
    implements
    PortableObject {
    public static final String ID_COLUMN_NAME = "__ID_KOL_NAME__";

    public static class InterpolationVariable {
        public String name;
        public String dataType;

        @Override
        public int hashCode() {
            int hashCode = (name == null ? 1 : name.hashCode());
            hashCode = hashCode + 31 * (dataType == null ? 7 : dataType.hashCode());
            return hashCode;
        }

        @Override
        public boolean equals(Object obj) {
            if ( obj == null ) {
                return false;
            }
            if ( !(obj instanceof InterpolationVariable) ) {
                return false;
            }
            InterpolationVariable that = (InterpolationVariable) obj;
            return equalOrNull( this.name,
                                that.name ) && equalOrNull( this.dataType,
                                                            that.dataType );
        }

        private boolean equalOrNull(Object lhs,
                                    Object rhs) {
            if ( lhs == null && rhs == null ) {
                return true;
            }
            if ( lhs != null && rhs == null ) {
                return false;
            }
            if ( lhs == null && rhs != null ) {
                return false;
            }
            return lhs.equals( rhs );
        }

    }

    private long                      idCol     = 0;
    private Map<String, List<String>> table     = new HashMap<String, List<String>>();
    private int                       rowsCount = 0;

    public int getColsCount() {
        return getInterpolationVariables().size() - 1;
    }

    public int getRowsCount() {
        return rowsCount;
    }

    private String getNewIdColValue() {
        idCol++;
        return String.valueOf( idCol );
    }

    public String addRow(String[] row) {
        return addRow( null,
                       row );
    }

    public String addRow(String rowId,
                         String[] row) {
        Map<InterpolationVariable, Integer> vars = getInterpolationVariables();
        if ( row.length != vars.size() - 1 ) {
            throw new IllegalArgumentException( "Invalid numbers of columns: " + row.length + " expected: "
                                                + vars.size() );
        }
        if ( rowId == null || rowId.length() == 0 ) {
            rowId = getNewIdColValue();
        }
        for ( Map.Entry<InterpolationVariable, Integer> entry : vars.entrySet() ) {
            List<String> list = table.get( entry.getKey() );
            if ( list == null ) {
                list = new ArrayList<String>();
                table.put( entry.getKey().name,
                           list );
            }
            if ( rowsCount != list.size() ) {
                throw new IllegalArgumentException( "invalid list size for " + entry.getKey() + ", expected: "
                                                    + rowsCount + " was: " + list.size() );
            }
            if ( ID_COLUMN_NAME.equals( entry.getKey() ) ) {
                list.add( rowId );
            } else {
                list.add( row[entry.getValue()] );
            }
        }
        rowsCount++;
        return rowId;
    }

    public boolean removeRowById(String rowId) {
        int idx = table.get( ID_COLUMN_NAME ).indexOf( rowId );
        if ( idx != -1 ) {
            for ( List<String> col : table.values() ) {
                col.remove( idx );
            }
            rowsCount--;
        }
        return idx != -1;
    }

    public void removeRow(int row) {
        if ( row >= 0 && row < rowsCount ) {
            for ( List<String> col : table.values() ) {
                col.remove( row );
            }
            rowsCount--;
        } else {
            throw new ArrayIndexOutOfBoundsException( row );
        }
    }

    public void clearRows() {
        if ( rowsCount > 0 ) {
            for ( List<String> col : table.values() ) {
                col.clear();
            }
            rowsCount = 0;
        }
    }

    public void putInSync() {
        
        //vars.KeySet is a set of InterpolationVariable, whereas table.keySet is a set of String
        Map<InterpolationVariable, Integer> vars = getInterpolationVariables();
        
        // Retain all columns in table that are in vars
        Set<String> requiredVars = new HashSet<String>();
        for(InterpolationVariable var : vars.keySet()) {
            if(table.containsKey( var.name )) {
                requiredVars.add(var.name);
            }
        }
        table.keySet().retainAll( requiredVars );

        // Add empty columns for all vars that are not in table
        List<String> aux = new ArrayList<String>( rowsCount );
        for ( int i = 0; i < rowsCount; i++ ) {
            aux.add( "" );
        }
        for(InterpolationVariable var : vars.keySet()) {
            if(!requiredVars.contains( var.name )) {
                table.put( var.name,
                           new ArrayList<String>( aux ) );
            }
        }
        
    }

    public InterpolationVariable[] getInterpolationVariablesList() {
        Map<InterpolationVariable, Integer> vars = getInterpolationVariables();
        InterpolationVariable[] ret = new InterpolationVariable[vars.size() - 1];
        for ( Map.Entry<InterpolationVariable, Integer> entry : vars.entrySet() ) {
            if ( !ID_COLUMN_NAME.equals( entry.getKey().name ) ) {
                ret[entry.getValue()] = entry.getKey();
            }
        }
        return ret;
    }

    private Map<InterpolationVariable, Integer> getInterpolationVariables() {
        Map<InterpolationVariable, Integer> result = new HashMap<InterpolationVariable, Integer>();
        new RuleModelVisitor( result ).visit( this );

        InterpolationVariable id = new InterpolationVariable();
        id.name = ID_COLUMN_NAME;
        result.put( id,
                    result.size() );
        return result;
    }

    public Map<String, List<String>> getTable() {
        return table;
    }

    public String[][] getTableAsArray() {
        if ( rowsCount <= 0 ) {
            return new String[0][0];
        }

        //Refresh against interpolation variables
        putInSync();
        
        String[][] ret = new String[rowsCount][table.size() - 1];
        Map<InterpolationVariable, Integer> vars = getInterpolationVariables();
        for ( Map.Entry<InterpolationVariable, Integer> entry : vars.entrySet() ) {
            InterpolationVariable var = entry.getKey();
            String varName = var.name;
            if ( ID_COLUMN_NAME.equals( varName ) ) {
                continue;
            }
            int idx = entry.getValue();
            for ( int row = 0; row < rowsCount; row++ ) {
                ret[row][idx] = table.get( varName ).get( row );
            }
        }
        return ret;
    }

    public void setValue(String varName,
                         int rowIndex,
                         String newValue) {
        getTable().get( varName ).set( rowIndex,
                                       newValue );
    }

    public static class RuleModelVisitor {

        private Map<InterpolationVariable, Integer> vars;

        public RuleModelVisitor(Map<InterpolationVariable, Integer> vars) {
            this.vars = vars;
        }

        public void visit(Object o) {
            if ( o == null ) {
                return;
            }
            if ( o instanceof RuleModel ) {
                visitRuleModel( (RuleModel) o );
            } else if ( o instanceof FactPattern ) {
                visitFactPattern( (FactPattern) o );
            } else if ( o instanceof CompositeFieldConstraint ) {
                visitCompositeFieldConstraint( (CompositeFieldConstraint) o );
            } else if ( o instanceof SingleFieldConstraint ) {
                visitSingleFieldConstraint( (SingleFieldConstraint) o );
            } else if ( o instanceof CompositeFactPattern ) {
                visitCompositeFactPattern( (CompositeFactPattern) o );
            } else if ( o instanceof FreeFormLine ) {
                visitFreeFormLine( (FreeFormLine) o );
            } else if ( o instanceof FromAccumulateCompositeFactPattern ) {
                visitFromAccumulateCompositeFactPattern( (FromAccumulateCompositeFactPattern) o );
            } else if ( o instanceof FromCollectCompositeFactPattern ) {
                visitFromCollectCompositeFactPattern( (FromCollectCompositeFactPattern) o );
            } else if ( o instanceof FromCompositeFactPattern ) {
                visitFromCompositeFactPattern( (FromCompositeFactPattern) o );
            } else if ( o instanceof DSLSentence ) {
                visitDSLSentence( (DSLSentence) o );
            } else if ( o instanceof ActionFieldList ) {
                visitActionFieldList( (ActionFieldList) o );
            }
        }

        private void visitActionFieldList(ActionFieldList afl) {
            for ( ActionFieldValue afv : afl.fieldValues ) {
                if ( afv.nature == FieldNature.TYPE_TEMPLATE && !vars.containsKey( afv.value ) ) {
                    InterpolationVariable var = new InterpolationVariable();
                    var.name = afv.value;
                    var.dataType = afv.type;
                    vars.put( var,
                              vars.size() );
                }
            }
        }

        public void visitRuleModel(RuleModel model) {
            if ( model.lhs != null ) {
                for ( IPattern pat : model.lhs ) {
                    visit( pat );
                }
            }
            if ( model.rhs != null ) {
                for ( IAction action : model.rhs ) {
                    visit( action );
                }
            }
        }

        private void visitFactPattern(FactPattern pattern) {
            for ( FieldConstraint fc : pattern.getFieldConstraints() ) {
                visit( fc );
            }
        }

        private void visitCompositeFieldConstraint(CompositeFieldConstraint cfc) {
            if ( cfc.constraints != null ) {
                for ( FieldConstraint fc : cfc.constraints ) {
                    visit( fc );
                }
            }
        }

        private void visitSingleFieldConstraint(SingleFieldConstraint sfc) {
            if ( BaseSingleFieldConstraint.TYPE_TEMPLATE == sfc.getConstraintValueType() && !vars.containsKey( sfc.getValue() ) ) {
                InterpolationVariable var = new InterpolationVariable();
                var.name = sfc.getValue();
                var.dataType = sfc.getFieldType();
                vars.put( var,
                          vars.size() );
            }
        }

        private void visitFreeFormLine(FreeFormLine ffl) {
            parseStringPattern( ffl.text );
        }

        private void visitCompositeFactPattern(CompositeFactPattern pattern) {
            if ( pattern.getPatterns() != null ) {
                for ( IFactPattern fp : pattern.getPatterns() ) {
                    visit( fp );
                }
            }
        }

        private void visitFromCompositeFactPattern(FromCompositeFactPattern pattern) {
            visit( pattern.getFactPattern() );
            parseStringPattern( pattern.getExpression().getText() );
        }

        private void visitFromCollectCompositeFactPattern(FromCollectCompositeFactPattern pattern) {
            visit( pattern.getFactPattern() );
            visit( pattern.getRightPattern() );
        }

        private void visitFromAccumulateCompositeFactPattern(FromAccumulateCompositeFactPattern pattern) {
            visit( pattern.getFactPattern() );
            visit( pattern.getSourcePattern() );

            parseStringPattern( pattern.getActionCode() );
            parseStringPattern( pattern.getInitCode() );
            parseStringPattern( pattern.getReverseCode() );
        }

        private void visitDSLSentence(final DSLSentence sentence) {
            parseStringPattern( sentence.sentence );
        }

        private void parseStringPattern(String text) {
            if ( text == null || text.length() == 0 ) {
                return;
            }
            int pos = 0;
            while ( (pos = text.indexOf( "@{",
                                         pos )) != -1 ) {
                int end = text.indexOf( '}',
                                        pos + 2 );
                if ( end != -1 ) {
                    String varName = text.substring( pos + 2,
                                                     end );
                    pos = end + 1;
                    InterpolationVariable var = new InterpolationVariable();
                    var.name = varName;
                    if ( !vars.containsKey( var ) ) {
                        vars.put( var,
                                  vars.size() );
                    }
                }
            }
        }
    }
}