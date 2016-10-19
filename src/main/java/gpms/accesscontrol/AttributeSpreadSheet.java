package gpms.accesscontrol;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

/***
 * Handles Attributes Dictionary stored in Excel File
 * 
 * @author milsonmunakami
 *
 */
public class AttributeSpreadSheet {

	private static List<AttributeRecord> allAttributeRecords = new ArrayList<AttributeRecord>();
	private HSSFWorkbook workBook1;

	public List<AttributeRecord> getAllAttributeRecords() {
		return allAttributeRecords;
	}

	/***
	 * Loads Attribute Dictionary for Access Control Evaluation
	 * 
	 * @param spreadSheetFile
	 *            Attribute Dictionary Excel File
	 * @throws Exception
	 */
	public AttributeSpreadSheet(InputStream spreadSheetFile) throws Exception {
		workBook1 = new HSSFWorkbook(spreadSheetFile);
		Sheet sheet = workBook1.getSheetAt(0);
		for (Row row : sheet) {
			loadAttributeRow(row);
		}
	}

	/***
	 * Loads the Attributes from the Attribute Dictionary Excel Metadata info
	 * 
	 * @param row
	 *            Row of Excel
	 */
	private static void loadAttributeRow(Row row) {
		if (row.getRowNum() != 0) {
			if (row.getCell(0) == null || row.getCell(1) == null)
				return;
			String attributeName = row.getCell(0).toString().trim();
			if (attributeName.equals("") || attributeName.startsWith("//"))
				return;
			String fullAttributeName = row.getCell(1) != null ? row.getCell(1)
					.toString() : "";

			String category = row.getCell(2) != null ? row.getCell(2)
					.toString() : "";
			String dataType = row.getCell(3) != null ? row.getCell(3)
					.toString() : "";
			String values = row.getCell(4) != null ? row.getCell(4).toString()
					: "";
			AttributeRecord attributeRecord = new AttributeRecord(
					attributeName, fullAttributeName, category, dataType,
					values);
			if (!allAttributeRecords.contains(attributeRecord)) {
				allAttributeRecords.add(attributeRecord);
			}
		}
	}

	/***
	 * Checks Attributes during Access Evaluation
	 * 
	 * @param attributeName
	 *            Attribute Name
	 * @return
	 */
	public AttributeRecord findAttributeRecord(String attributeName) {
		for (AttributeRecord record : allAttributeRecords) {
			if (record.getAttributeName().equalsIgnoreCase(attributeName))
				return record;
		}
		return null;
	}
}
