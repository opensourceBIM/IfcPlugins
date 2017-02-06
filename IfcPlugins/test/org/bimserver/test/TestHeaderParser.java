package org.bimserver.test;

import java.text.ParseException;

import org.bimserver.ifc.step.deserializer.IfcHeaderParser;
import org.bimserver.ifc.step.deserializer.TokenizeException;
import org.bimserver.models.store.IfcHeader;
import org.bimserver.plugins.deserializers.DeserializeException;
import org.junit.Assert;
import org.junit.Test;

public class TestHeaderParser {
	@Test
	public void test1() {
		try {
			IfcHeader ifcHeader = new IfcHeaderParser().parseFileName("('\\\\alpha\\macvol\\Projects\\2006\\06006 18 - 40 Mount St\\11.0 CAD\\11.20 Data Exchange\\Sent out\\IFC''s\\090320\\A.BIM.P-090320.ifc','2009-03-20T16:36:54',('Architect'),('Building Designer Office'),'PreProc - EDM 4.5.0033','Windows System','The authorising person')");
			// TODO do something with ifcHeader
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void test2() {
		try {
			new IfcHeaderParser().parseFileName(
					"(\r\n/* name */ '040123_TF_Teil_Halle_A3',\r\n/* time_stamp */ '2004-01-23T12:53:15+01:00',\r\n/* author */ ('Dayal'),\r\n/* organization */ ('Audi/TUM'),\r\n/* preprocessor_version */ 'ST-DEVELOPER v8',\r\n/* originating_system */ 'WinXP',\r\n/* authorisation */ 'dayal')");
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void test3() {
		try {
			new IfcHeaderParser().parseFileName("('', '2007-04-10T13:03:07', (''), (''), 'IFC Export', 'Esa.Pt', '')");
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void test4() {
		try {
			new IfcHeaderParser().parseFileName("($,'2013-05-02T10:04:35',(''),(''),'Autodesk Revit Architecture 2013','20120221_2030(x64)','')");
		} catch (DeserializeException e) {
			Assert.assertEquals("FILE_NAME.name is not an optional field, but $ used", e.getMessage());
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void test5() {
		try {
			new IfcHeaderParser().parseFileName("('G:\\Users\\NLST\\ArchiCAD\\2x.ifc','2006-02-16T17:26:18',('Architect'),('Building Designer Office'),'PreProc - EDM 4.5.0033','Windows System','The authorising person')");
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void test6() {
		try {
			new IfcHeaderParser().parseDescription("(('ArchiCAD 11.00 Release 1 generated IFC file.','Build Number of the Ifc 2x3 interface: 63096 (01-09-2008)\\X\\0A'),'2;1')");
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void test7() {
		try {
			new IfcHeaderParser().parseDescription("(('ArchiCAD 11.00 Release 1 generated IFC file.','Build Number of the Ifc 2x3 interface: 63090 (13-06-2008)\\X\\0A'),'2;1')");
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void test8() {
		try {
			new IfcHeaderParser().parseDescription("((''), '2;1')");
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void test9() {
		try {
			new IfcHeaderParser().parseDescription("((), '2;1')");
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void test10() {
		try {
			new IfcHeaderParser().parseFileName(
					"('Y:\\IFC\\IFC Certification\\IFC2x3 ADT Files\\Ready for IAI\\01-01-03-Clipping-ADT.ifc','2006-12-12T10:07:32',('Autodesk Inc.'),('Autodesk Inc.',''),'AutoCAD Architecture Kiasma Build 17.1.40.0 - 1.0','Microsoft Windows NT 5.1.2600 Service Pack 2','')");
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void test11() {
		try {
			new IfcHeaderParser().parseFileName(
					"('C:\\documents and settings\\stephj1\\my documents\\briefcases\\ifc-mbomb\\ifc-mbomb_t416\\t-block\\Views\\003-T-Block.dwg','2004-01-26T14:03:27',(''),('Taylor Woodrow'),'IFC-Utility 2x for ADT V. 2, 0, 2, 5   (www.inopso.com) - IFC Toolbox Version 2.x (00/11/07)','Autodesk Architectural Desktop','JS')");
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void test12() {
		try {
			new IfcHeaderParser().parseFileName(
					"('C:\\IFC\\IFC Certification\\IFC2x3 ADT Files\\Ready for IAI\\00-01-01-BasicSpaces-ADT-fix1.ifc','2006-12-14T10:55:37',('Autodesk Inc.'),('Autodesk Inc.',''),'AutoCAD Architecture Kiasma Build 17.1.40.0 - 1.0','Microsoft Windows NT 5.1.2600 Service Pack 2','')");
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void test13() {
		try {
			new IfcHeaderParser().parseFileName("('WallIFCexport_situationzelfdeguids.ifc','2013-06-27T20:05:58',(''),(''),'Autodesk Revit 2013','20121003_2115(x64) - Exporter 2.7.0.0 - Alternate UI 1.7.0.0',$)");
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void test14() {
		try {
			new IfcHeaderParser().parseDescription("(('ArchiCAD 11.00 Release 1 generated IFC file.','Build Number of the Ifc 2x3 interface: 63090 (13-06-2008)\\X\\0A'),'2;1')");
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void test15() {
		try {
			new IfcHeaderParser().parseFileName(
					"('S:\\[IFC]\\COMPLETE-BUILDINGS\\FZK-MODELS\\Buerogebaeude-Zones\\ArchiCAD-11\\Institute-Var-2\\IFC2x3\\AC11-Institute-Var-2-IFC.ifc','2008-07-03T15:22:43',('Architect'),('Building Designer Office'),'PreProc - EDM 4.5.0033','Windows System','The authorising person')");
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void test16() {
		try {
			new IfcHeaderParser().parseFileName(
					"('V:\\R\\S\\E\\S\\/zn\\S\\C\\S\\)\\\\Proteo\\\\Nuselsk\\S\\C\\S\\= most BIM.14003\\\\Pracovn\\S\\C\\S\\-\\\\Martin\\\\IFC test\\\\6\\\\mal\\S\\C\\S\\= model - fid jako ifc tag NAME.ifc','2014-10-13T12:28:40',('Architect'),('Building Designer Office'),'PreProc - EDM 5.0','IFC file generated by Graphisoft ArchiCAD-64 17.0.0 CZE FULL Windows version (IFC2x3 add-on version: 6004 CZE FULL).','The authorising person')");
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void test17() {
		try {
			new IfcHeaderParser().parseFileName(
					"('/Volumes/KIM-HD-001/Projects/IAI/\\S\\c\\X\\82\\S\\$\\S\\c\\X\\83\\S\\3\\S\\c\\X\\83\\X\\95\\S\\c\\X\\82\\X\\9A\\S\\c\\X\\83\\S\\*\\S\\e\\X\\88\\X\\86\\S\\g\\S\\'\\X\\91\\S\\d\\S\\<\\X\\9A/20130419_\\S\\f\\X\\96\\X\\87\\S\\e\\S\\-\\X\\97\\S\\c\\X\\82\\S\\3\\S\\c\\X\\83\\S\\<\\S\\c\\X\\83\\X\\88\\S\\c\\X\\82\\X\\99\\S\\c\\X\\82\\S\\5\\S\\c\\X\\83\\S\\3\\S\\c\\X\\83\\X\\95\\S\\c\\X\\82\\X\\9A\\S\\c\\X\\83\\S\\+/AC16_sjis-VW-sjis.ifc','2013-04-19T12:38:54',('Architect'),('Nemetschek Vectorworks, Inc.'),'Vectorworks Architect 2013 SP3 (Build 183378) by Nemetschek Vectorworks, Inc.','Macintosh','')");
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void test21() {
		try {
			new IfcHeaderParser().parseFileName("('TBlockArchicadDucts',\r\n     '2004-01-22T20:08:03',\r\n          ('sdai-user'),\r\n          ('ANONYMOUS ORGANISATION'),\r\n          'EXPRESS Data Manager version 20030909',   \r\n       $,\r\n          $)");
		} catch (TokenizeException e) {
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
}
