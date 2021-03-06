package org.compiere.grid;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

import org.compiere.minigrid.IMiniTable;
import org.compiere.model.GridTab;
import org.compiere.model.I_C_UOM;
import org.compiere.model.MAttributeSetInstance;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MProduct;
import org.compiere.model.Query;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;
import org.compiere.util.Msg;
import org.eevolution.model.MPPProductBOM;
import org.eevolution.model.MPPProductBOMLine;

/**
 * Create Order from Material requirement
 * 
 * @author noum
 * 
 */
public class VCreateFromOrder extends CreateFrom {

	public VCreateFromOrder(GridTab gridTab) {
		super(gridTab);
		log.info(gridTab.toString());
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean dynInit() throws Exception {
		// TODO Auto-generated method stub
		setTitle(Msg.getElement(Env.getCtx(), "C_Order_ID", false) + " .. "
				+ Msg.translate(Env.getCtx(), "CreateFrom"));
		return true;
	}

	@Override
	public void info() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean save(IMiniTable miniTable, String trxName) {
		// TODO Auto-generated method stub
		Properties ctx = Env.getCtx();
		int C_Order_ID = ((Integer) getGridTab().getValue("C_Order_ID"))
				.intValue();
		System.out.println("create order line of  ...." + C_Order_ID);
		// Lines .. create order line and save it
//		List<MOrderLine> newOrderLineList = new ArrayList<MOrderLine>();
		List<MOrderLine> dupLine = new ArrayList<MOrderLine>();
		for (int i = 0; i < miniTable.getRowCount(); i++) {
			if (((Boolean) miniTable.getValueAt(i, 0)).booleanValue()) {
				KeyNamePair pp = (KeyNamePair) miniTable.getValueAt(i, 1);
				
				KeyNamePair attSetInsPair = (KeyNamePair)miniTable.getValueAt(i,2);
				int M_AttributeSetInstance_ID = attSetInsPair.getKey();
				
				int M_Product_ID = pp.getKey();
				MProduct product = new MProduct(ctx, M_Product_ID, null);
//				product.getc
				MOrder order = new MOrder(ctx, C_Order_ID, null);
				MOrderLine orderLine = new MOrderLine(order);
				// orderLine.setC_UOM_ID(100); // uom_id: 100 - Each
				
				orderLine.setM_Product_ID(M_Product_ID);
				
				KeyNamePair pq = (KeyNamePair) miniTable.getValueAt(i, 4);
				int C_UOM_Purchase_ID = pq.getKey();
				System.out.println(pq.getKey() + " " + pq.getName());
				orderLine.setC_UOM_ID(C_UOM_Purchase_ID);
				
				// String desc = (String)miniTable.getValueAt(i, 5); aon 12/3/56
				// orderLine.setDescription(desc);  aon 12/3/56
				
				if(M_AttributeSetInstance_ID != 0){
					orderLine.setM_AttributeSetInstance_ID(M_AttributeSetInstance_ID);
				}
									
				if(duplicateOrderLine(order, orderLine)){
					dupLine.add(orderLine);
				}else{
					orderLine.save();
				}					
			}
		}
		StringBuilder msg = new StringBuilder();
		if(dupLine.size() != 0){
			msg.append("���͡��¡�ë��\n");
			for(MOrderLine line :dupLine){
				
				msg.append("�ѵ�شԺ ["+ line.getM_Product_ID()+ " " + line.getProduct().getName() + "] " +
						"��������´ [" + line.getM_AttributeSetInstance_ID()+ " " + line.getM_AttributeSetInstance().getDescription()+ "]\n");
			}
			JOptionPane.showMessageDialog(null, msg);
		}
		
		return true;
	}
	private boolean duplicateOrderLine(MOrder order, MOrderLine newLine){
		boolean dup = true;
		for (MOrderLine mOrderLine : order.getLines()) {
			if(mOrderLine.equals(newLine)){
				return dup;
			}
		}
		return false;
	}

	protected Vector<String> getBOMColumnNames() {
		Vector<String> columnNames = new Vector<String>(7);
		columnNames.add(Msg.getMsg(Env.getCtx(), "���͡"));
		columnNames.add(Msg.translate(Env.getCtx(), "�����ѵ�شԺ"));
		columnNames.add(Msg.translate(Env.getCtx(), "��͸Ժ��"));
		columnNames.add("�ӹǹ��͵��");								// columnNames.add("Qty Required");
		columnNames.add("˹���");
		columnNames.add(Msg.translate(Env.getCtx(), "��觫�������ش")); // ��� column 2-aon 12/3/56
		return columnNames;
	}

	protected void configureMiniTable(IMiniTable miniTable) {
		miniTable.setColumnClass(0, Boolean.class, false);   // 0-Selection
		miniTable.setColumnClass(1, String.class, true);     // 1-Product
		miniTable.setColumnClass(2, String.class, true);     // 3-AttributeSetInstance_ID
		miniTable.setColumnClass(3, BigDecimal.class, true); // 3-Qty	// miniTable.setColumnClass(5, BigDecimal.class, true); // 4-Qty Req.
		miniTable.setColumnClass(4, String.class, true);     // 5-UOM
		miniTable.setColumnClass(5, String.class, true);     // 2-Description
		miniTable.autoSize();
	}

	protected Vector<Vector<Object>> getBOMData(int M_Product_ID) {
		Vector<Vector<Object>> data = new Vector<Vector<Object>>();
		// String sql =
		// "select pp_product_bom_id from pp_product_bom where isbom_mrq = 'Y' and m_product_id = "
		// + M_Product_ID;
		String sql = "select pp_product_bom_id from pp_product_bom "
				+ "where 	isbom_mrq = 'Y' and isactive = 'Y' "
				+ "and m_product_id = " + M_Product_ID
				+ " and updated = ( select max(updated) "
				+ "from pp_product_bom "
				+ "where 	isbom_mrq = 'Y' and isactive = 'Y' "
				+ "and m_product_id = " + M_Product_ID + " )";
		log.info("========");
		log.info(sql);
		log.info("========");

		int PP_Product_BOM_ID = DB.getSQLValue(null, sql);
		System.out.println(PP_Product_BOM_ID);

		MPPProductBOM productBOM = new MPPProductBOM(Env.getCtx(),
				PP_Product_BOM_ID, null);
		System.out.println(productBOM.getName());

		MPPProductBOMLine[] bomLines = productBOM.getLines();
		for (int i = 0; i < bomLines.length; i++) {
			MPPProductBOMLine bomLine = bomLines[i];
			MProduct product = (MProduct) bomLine.getM_Product();
			System.out.println(product.getName());
			Vector<Object> line = new Vector<Object>();
			// 0-Selection
			line.add(new Boolean(false));
			
			KeyNamePair pp = new KeyNamePair(product.get_ID(),
					product.getName());
			// 1-Product
			line.add(pp);
			
// aon --��Ѻ column 12/3/56 -----------		
			// 2-AttributeSetInstance_ID
			Properties ctx = Env.getCtx();  
			int M_AttributeSetInstance_ID = bomLine.getM_AttributeSetInstance_ID();    
			Query qry = new Query(ctx, "M_AttributeSetInstance", "M_AttributeSetInstance_ID = " + M_AttributeSetInstance_ID , null);
			MAttributeSetInstance mAttSetInstance = qry.first();
			String M_AttSetIns_Desc = mAttSetInstance.getDescription();
			KeyNamePair attSetIns = new KeyNamePair(M_AttributeSetInstance_ID, M_AttSetIns_Desc);
			line.add(attSetIns);
			
			// line.add(bomLine.getM_AttributeSetInstance_ID());
			
			// 3-Qty PP_Product_BOMLine.QtyBOM
			line.add(bomLine.getQty());
			
			// 5-Qty Req. PP_Product_BOMLine.QtyRequired
			// line.add(new BigDecimal(0));
			
			// 4-UOM	// line.add(product.getUOMSymbol());
			I_C_UOM uom  = bomLine.getC_UOM_Purchase(); 
			System.out.println(uom.getC_UOM_ID() +" " +uom.getName());
			KeyNamePair pq = new KeyNamePair(uom.getC_UOM_ID(),uom.getName());
			line.add(pq);
			
			// 5-BomLine Description
			// line.add(bomLine.getDescription());    12/3/56
			// aon 12/3/56			
			//Properties ctx = Env.getCtx();
			//int M_AttributeSetInstance_ID = bomLine.getM_AttributeSetInstance_ID();

			String sql2 = "select description from m_attributesetinstance_lastpo_v_pt "
					+ "where m_attributesetinstance_id = " + M_AttributeSetInstance_ID;

			String descpo = DB.getSQLValueString("description", sql2);
			//System.out.println(descpo);
			line.add(descpo);			
			
			data.add(line);
		}

		return data;
	}
}
