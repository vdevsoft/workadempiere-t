/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2006 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
package org.tkgarment.process;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.logging.Level;

import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.adempiere.exceptions.DBException;
// import org.compiere.model.MAging;
import org.compiere.model.MRole;
import org.compiere.util.DB;
import org.compiere.util.TimeUtil;



/**
 *	Product Card Sum Report.
 *	Based on RV_T_Productcardsum.
 *  @author Prapon Thavornkaew 11/03/2015
 */
public class ProductCardSumReport extends SvrProcess
{
	/** The date to calculate the days due from			*/
	/*
	private Timestamp	p_StatementDate = null;
	//FR 1933937
	private boolean		p_DateAcct = false;
	private boolean 	p_IsSOTrx = false;
	private int			p_C_Currency_ID = 0;
	private int			p_AD_Org_ID = 0;
	private int			p_C_BP_Group_ID = 0;
	private int			p_C_BPartner_ID = 0;
	private boolean		p_IsListInvoices = false;
	*/
	private int 		p_ad_pinstance_id; 
	private String      p_Ponum; 
	private Timestamp 	p_MovementDatefrom; 
	private Timestamp   p_MovementDateto; 
	private int 		p_M_Product_Categoty_ID = 0; 
	private int 		p_M_Product_ID = 0; 
	private int			p_SalesRep_ID = 0;

	
	
	/** Number of days between today and statement date	*/
	// private int			m_statementOffset = 0;
	
	/**
	 *  Prepare - e.g., get Parameters.
	 */
	protected void prepare()
	{
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;
			else if (name.equals("'PONum"))
				p_Ponum = (String)para[i].getParameter();				
			else if (name.equals("MovementDate")) {
				p_MovementDatefrom = (Timestamp)para[i].getParameter();
				p_MovementDateto = (Timestamp)para[i].getParameter_To();			
			} 
			else if (name.equals("M_Product_Categoty_ID"))
				p_M_Product_Categoty_ID = ((BigDecimal)para[i].getParameter()).intValue();
			else if (name.equals("M_Product_ID"))
				p_M_Product_ID = ((BigDecimal)para[i].getParameter()).intValue();
			else if (name.equals("SalesRep_ID"))
				p_SalesRep_ID = ((BigDecimal)para[i].getParameter()).intValue();
	//		else
	//			log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}
		if (p_MovementDatefrom == null)
			p_MovementDatefrom = new Timestamp (System.currentTimeMillis());
		if (p_MovementDateto == null) 
			p_MovementDateto = new Timestamp (System.currentTimeMillis());
	    System.out.println(" date :" + p_MovementDatefrom + p_MovementDateto);
	}	//	prepare

	/**
	 * 	DoIt
	 *	@return Message
	 *	@throws Exception
	 */
	protected String doIt() throws Exception
	{
		log.info("PONum=" + p_Ponum + ", MovementDate=" + p_MovementDatefrom
			+ ", MovementDateto=" + p_MovementDateto + ", M_Product_Categoty_ID=" + p_M_Product_Categoty_ID
			+ ", M_Product_ID=" + p_M_Product_ID + ", SalesRep_ID=" + p_SalesRep_ID);

		// String datefrom = DB.TO_DATE(p_MovementDatefrom);  
		// String dateto   = DB.TO_DATE(p_MovementDateto);  
		//String dateto   = (String)p_MovementDateto;  
		 
		int AD_PInstance_ID = getAD_PInstance_ID();
		//int AD_User_ID = getAD_User_ID();
		//String sql = "select transaction_cardsum_pt( "+AD_PInstance_ID+", '" + p_MovementDatefrom + "', '" + p_MovementDateto + "') as found";
                	    	    
	    String sql = "insert into t_transaction_sum (ad_pinstance_id,ad_client_id,ad_org_id,created,createdby,updated,updatedby,isactive,m_product_id,m_product_category_id"
	    	        +",movementdate,ponum,qtybringforward,qtyin,qtyout,costaverage)"
	                +" select "+AD_PInstance_ID+", a.ad_client_id, a.ad_org_id, getdate(),"+100+", getdate(),"+100+",'Y'"
	                +  ",a.m_product_id,pd.m_product_category_id,'"+p_MovementDatefrom+"', a.ponum,0,0,0,0.00"
	                +" from ("    
	                +  " select  T.ad_client_id, T.ad_org_id, T.m_product_id, T.ponum" 
	                +  " from ("
	                +    " select t.ad_client_id, t.ad_org_id, t.m_product_id, COALESCE(L.ponum,atsi.lot) as ponum"
	                +    " from m_transaction t"
	                +    " left join m_attributesetinstance atsi ON t.m_attributesetinstance_id = atsi.m_attributesetinstance_id"
	                +    " left join t_ProductLotPrice_v_pt L ON t.m_product_id = L.m_product_id and t.m_attributesetinstance_id = L.m_attributesetinstance_id" 
	                +    " where t.movementdate <= '"+p_MovementDateto+"'"; 
        if (p_M_Product_ID != 0)
           sql = sql +     " and t.m_product_id="+p_M_Product_ID; // if not empty
        sql = sql +    ") T";
        if (p_Ponum != null)
           sql = sql+ " where T.ponum="+p_Ponum; // if not empty
	    sql = sql +   " group by T.ad_client_id, T.ad_org_id, T.m_product_id, T.ponum"
	              +   " order by T.ad_client_id, T.ad_org_id, T.m_product_id, T.ponum"
	              +  " )a"
	              + " left join m_product pd ON a.m_product_id = pd.m_product_id";
   	    
    	int f1 = DB.getSQLValue("found", sql);
	    System.out.println(" Found1 :" + f1);		
		
	    String sql2 = "select transaction_cardsum_pt( "+AD_PInstance_ID+", '" + p_MovementDatefrom + "', '" + p_MovementDateto + "') as found";
    	int f2 = DB.getSQLValue("found", sql2);
	    System.out.println(" Found2:" + f2);		

		return "";
	}	//	doIt

}	//	Aging
