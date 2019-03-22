/*
* File Name : SelectParseListener.java
* Author : mixkorea
* Date : 2019-03-19
* Description : File description
* 
* Modification Information
* Date         Reviser      Description
* ------------ ----------- ---------------------------
* 2019. 3. 19. mixkorea     최초작성
* 2019. 3. 19. mixkorea     수정사항 입력
*
* 상기 프로그램에 대한 저작권을 포함한 지적재산권은 에스코어(주)에 있으며,
* 에스코어(주)가 명시적으로 허용하지 않은 사용, 복사, 변경, 제3자에의 공개,
* 배포는 엄격히 금지되며, 에스코어(주)의 지적 재산권 침해에 해당됩니다.
* (Copyright ⓒ 2014 S-Core Co., Ltd. All Rights Reserved| Confidential)
*/
package com.restlet.sqlimport.parser;


import com.restlet.sqlimport.model.sql.Column;
import com.restlet.sqlimport.model.sql.Database;
import com.restlet.sqlimport.model.sql.ForeignKey;
import com.restlet.sqlimport.model.sql.Table;
import com.restlet.sqlimport.parser.SqlParser.Any_nameContext;
import com.restlet.sqlimport.parser.SqlParser.Column_constraint_not_nullContext;
import com.restlet.sqlimport.parser.SqlParser.Column_constraint_primary_keyContext;
import com.restlet.sqlimport.parser.SqlParser.Column_defContext;
import com.restlet.sqlimport.parser.SqlParser.Column_default_valueContext;
import com.restlet.sqlimport.parser.SqlParser.Column_nameContext;
import com.restlet.sqlimport.parser.SqlParser.Fk_origin_column_nameContext;
import com.restlet.sqlimport.parser.SqlParser.Fk_target_column_nameContext;
import com.restlet.sqlimport.parser.SqlParser.Foreign_key_clauseContext;
import com.restlet.sqlimport.parser.SqlParser.Foreign_tableContext;
import com.restlet.sqlimport.parser.SqlParser.Indexed_columnContext;
import com.restlet.sqlimport.parser.SqlParser.NameContext;
import com.restlet.sqlimport.parser.SqlParser.Select_stmtContext;
import com.restlet.sqlimport.parser.SqlParser.Table_constraint_foreign_keyContext;
import com.restlet.sqlimport.parser.SqlParser.Table_constraint_primary_keyContext;
import com.restlet.sqlimport.parser.SqlParser.Table_nameContext;
import com.restlet.sqlimport.parser.SqlParser.Type_nameContext;
import com.restlet.sqlimport.parser.SqlParser.UnknownContext;
import com.restlet.sqlimport.util.Util;


/**
 * Method description
 *
 * @author mixkorea
 * @since  2019. 3. 19.
 * @version 
 * @see
 */
@SuppressWarnings("unused")
public class SelectParseListener extends SqlBaseListener {

    private static final boolean DEBUG                          = true;

    /**
     * ANTLR Parser
     */

    private final SqlParser      sqlParser;

    /**
     * Database schema
     */
    private final Database       database;

    private Table                table;

    private Column               column;
    private ForeignKey           foreignKey;

    /** Positions */
    private boolean              inCreateTable                  = false;     // CREATE TABLE
    private boolean              inColumnDef                    = false;     // Column definition
    private boolean              inTypeName                     = false;     // Column type in the column definition
    private boolean              inTable_constraint_primary_key = false;     // PRIMARY KEY
    private boolean              inTable_constraint_foreign_key = false;     // FOREIGN KEY

    /**
     * Utils methods.
     */
    Util                         util                           = new Util();


    /**
     * Used only for debug, its called for each token based on the token "name".
     */
    @Override
    public void exitAny_name(final Any_nameContext ctx) {
        if (DEBUG) {
            System.out.println(ctx.getText() + " - ctx : " + ctx.toInfoString(sqlParser));
        }
    }
    
    


    @Override
    public void exitUnknown(final UnknownContext ctx) {
        if (DEBUG) {
            System.out.println(ctx.getText() + " - ctx : " + ctx.toInfoString(sqlParser));
        }
    }


    /**
     * Constructor.
     * @param sqlParser SQL parser
     * @param database Database
     */
    public SelectParseListener(final SqlParser sqlParser, final Database database) {
        this.sqlParser = sqlParser;
        this.database = database;
    }


    @Override
    public void enterSelect_stmt(final Select_stmtContext ctx) {
        System.err.println(ctx);
    }


    @Override
    public void exitSelect_stmt(final Select_stmtContext ctx) {
        System.err.println(ctx);
    }


    /**
     * Table name
     */
    @Override
    public void exitTable_name(final Table_nameContext ctx) {
        if (inCreateTable) {
            table.setName(util.unformatSqlName(ctx.getText()));
        }
    }


    //--- Column definition

    /**
     * enter Column definition
     */
    @Override
    public void enterColumn_def(final Column_defContext ctx) {
        inColumnDef = true;
        if (inCreateTable) {
            column = new Column();
        }
    }


    /**
     * exit Column definition
     */
    @Override
    public void exitColumn_def(final Column_defContext ctx) {
        if (inCreateTable) {
            if ((column != null) && (column.getName() != null)) {
                table.getColumnByNames().put(column.getName(), column);
            }
            column = null;
        }
        inColumnDef = false;
    }


    /**
     * Column name
     */
    @Override
    public void exitColumn_name(final Column_nameContext ctx) {
        if (inCreateTable && inColumnDef) {
            column.setName(util.unformatSqlName(ctx.getText()));
        }
    }


    //--- Column type

    /**
     * enter Column type
     */
    @Override
    public void enterType_name(final Type_nameContext ctx) {
        inTypeName = true;
    }


    /**
     * exit column type
     */
    @Override
    public void exitType_name(final Type_nameContext ctx) {
        inTypeName = false;
    }


    /**
     * Name. It could be : <br/>
     * - type name
     */
    @Override
    public void exitName(final NameContext ctx) {
        if (inCreateTable && inColumnDef && inTypeName) {
            if (column.getType() == null) {
                column.setType(util.unformatSqlName(ctx.getText()));
            } else {
                column.setType(column.getType() + " " + util.unformatSqlName(ctx.getText()));
            }
        }
    }

    //--- Constraints


    //--- Default

    @Override
    public void exitColumn_default_value(final Column_default_valueContext ctx) {
        if (inCreateTable && inColumnDef) {
            column.setDefaultValue(util.unformatSqlName(ctx.getText()));
        }
    }


    //--- Not Null

    @Override
    public void exitColumn_constraint_not_null(final Column_constraint_not_nullContext ctx) {
        if (inCreateTable && inColumnDef) {
            column.setIsNotNull(true);
        }
    }


    //--- Primary Key in Column definition

    @Override
    public void exitColumn_constraint_primary_key(final Column_constraint_primary_keyContext ctx) {
        if (inCreateTable && inColumnDef) {
            table.getPrimaryKey().getColumnNames().add(column.getName());
        }
    }


    //--- Primary Key in CREATE TABLE or in ALTER TABLE

    @Override
    public void enterTable_constraint_primary_key(final Table_constraint_primary_keyContext ctx) {
        inTable_constraint_primary_key = true;
    }


    @Override
    public void exitTable_constraint_primary_key(final Table_constraint_primary_keyContext ctx) {
        inTable_constraint_primary_key = false;
    }


    @Override
    public void exitIndexed_column(final Indexed_columnContext ctx) {
        if (inCreateTable && inTable_constraint_primary_key) {
            final String columnName = util.unformatSqlName(ctx.getText());
            table.getPrimaryKey().getColumnNames().add(columnName);
        }
    }


    //--- Foreign Key in CREATE TABLE or in ALTER TABLE

    @Override
    public void enterTable_constraint_foreign_key(final Table_constraint_foreign_keyContext ctx) {
        setInTable_constraint_foreign_key(true);
        if (inCreateTable) {
            foreignKey = new ForeignKey();
            foreignKey.setTableNameOrigin(table.getName());
        }
    }


    @Override
    public void exitTable_constraint_foreign_key(final Table_constraint_foreign_keyContext ctx) {
        if (inCreateTable) {
            foreignKey.setTableNameOrigin(table.getName());
            table.getForeignKeys().add(foreignKey);
            foreignKey = null;
        }
        setInTable_constraint_foreign_key(false);
    }


    @Override
    public void enterForeign_key_clause(final Foreign_key_clauseContext ctx) {
        if (inCreateTable && inColumnDef) {
            foreignKey = new ForeignKey();
            foreignKey.setTableNameOrigin(table.getName());
            foreignKey.getColumnNameOrigins().add(column.getName());
        }
    }


    @Override
    public void exitForeign_key_clause(final Foreign_key_clauseContext ctx) {
        if (inCreateTable && inColumnDef) {
            foreignKey.setTableNameOrigin(table.getName());
            table.getForeignKeys().add(foreignKey);
            foreignKey = null;
        }
    }


    @Override
    public void exitForeign_table(final Foreign_tableContext ctx) {
        if (inCreateTable) {
            foreignKey.setTableNameTarget(util.unformatSqlName(ctx.getText()));
        }
    }


    @Override
    public void exitFk_origin_column_name(final Fk_origin_column_nameContext ctx) {
        if (foreignKey != null) {
            foreignKey.getColumnNameOrigins().add(util.unformatSqlName(ctx.getText()));
        }
    }


    @Override
    public void exitFk_target_column_name(final Fk_target_column_nameContext ctx) {
        if (inCreateTable) {
            foreignKey.getColumnNameTargets().add(util.unformatSqlName(ctx.getText()));
        }
    }


    /**
     * @return the inTable_constraint_foreign_key
     */
    public boolean isInTable_constraint_foreign_key() {
        return inTable_constraint_foreign_key;
    }


    /**
     * @param inTable_constraint_foreign_key the inTable_constraint_foreign_key to set
     */
    public void setInTable_constraint_foreign_key(boolean inTable_constraint_foreign_key) {
        this.inTable_constraint_foreign_key = inTable_constraint_foreign_key;
    }

}
