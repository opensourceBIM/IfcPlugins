package org.bimserver.test;

/******************************************************************************
 * Copyright (C) 2009-2019  BIMserver.org
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see {@literal<http://www.gnu.org/licenses/>}.
 *****************************************************************************/

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.bimserver.ifc.step.deserializer.IfcHeaderParser;
import org.bimserver.models.store.IfcHeader;
import org.bimserver.plugins.deserializers.DeserializeException;
import org.junit.Assert;
import org.junit.Test;

public class TestHeaderParser {
	@Test
	public void test1() {
		try {
			IfcHeader ifcHeader = new IfcHeaderParser().parseFileName("('\\\\alpha\\\\macvol\\\\Projects\\\\2006\\\\06006 18 - 40 Mount St\\\\11.0 CAD\\\\11.20 Data Exchange\\\\Sent out\\\\IFC''s\\\\090320\\\\A.BIM.P-090320.ifc','2009-03-20T16:36:54',('Architect'),('Building Designer Office'),'PreProc - EDM 4.5.0033','Windows System','The authorising person')", 0);
			
			Assert.assertEquals("\\alpha\\macvol\\Projects\\2006\\06006 18 - 40 Mount St\\11.0 CAD\\11.20 Data Exchange\\Sent out\\IFC's\\090320\\A.BIM.P-090320.ifc", ifcHeader.getFilename());

			GregorianCalendar gregorianCalendar = new GregorianCalendar();
			gregorianCalendar.set(Calendar.YEAR, 2009);
			gregorianCalendar.set(Calendar.MONTH, 2);
			gregorianCalendar.set(Calendar.DAY_OF_MONTH, 20);
			gregorianCalendar.set(Calendar.HOUR_OF_DAY, 16);
			gregorianCalendar.set(Calendar.MINUTE, 36);
			gregorianCalendar.set(Calendar.SECOND, 54);
			gregorianCalendar.set(Calendar.MILLISECOND, 0);

			Assert.assertEquals(gregorianCalendar.getTime(), ifcHeader.getTimeStamp());
			Assert.assertEquals("Architect", ifcHeader.getAuthor().get(0));
			Assert.assertEquals("Building Designer Office", ifcHeader.getOrganization().get(0));
			Assert.assertEquals("PreProc - EDM 4.5.0033", ifcHeader.getPreProcessorVersion());
			Assert.assertEquals("Windows System", ifcHeader.getOriginatingSystem());
			Assert.assertEquals("The authorising person", ifcHeader.getAuthorization());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void test2() {
		try {
			IfcHeader ifcHeader = new IfcHeaderParser().parseFileName(
					"(\r\n/* name */ '040123_TF_Teil_Halle_A3',\r\n/* time_stamp */ '2004-01-23T12:53:15+01:00',\r\n/* author */ ('Dayal'),\r\n/* organization */ ('Audi/TUM'),\r\n/* preprocessor_version */ 'ST-DEVELOPER v8',\r\n/* originating_system */ 'WinXP',\r\n/* authorisation */ 'dayal')", 0);

			Assert.assertEquals("040123_TF_Teil_Halle_A3", ifcHeader.getFilename());

			GregorianCalendar gregorianCalendar = new GregorianCalendar();
			gregorianCalendar.set(Calendar.YEAR, 2004);
			gregorianCalendar.set(Calendar.MONTH, 0);
			gregorianCalendar.set(Calendar.DAY_OF_MONTH, 23);
			gregorianCalendar.set(Calendar.HOUR_OF_DAY, 12);
			gregorianCalendar.set(Calendar.MINUTE, 53);
			gregorianCalendar.set(Calendar.SECOND, 15);
			gregorianCalendar.set(Calendar.MILLISECOND, 0);
			
			Assert.assertEquals(gregorianCalendar.getTime(), ifcHeader.getTimeStamp());
			Assert.assertEquals(1, ifcHeader.getAuthor().size());
			Assert.assertEquals("Dayal", ifcHeader.getAuthor().get(0));
			Assert.assertEquals(1, ifcHeader.getOrganization().size());
			Assert.assertEquals("Audi/TUM", ifcHeader.getOrganization().get(0));
			Assert.assertEquals("ST-DEVELOPER v8", ifcHeader.getPreProcessorVersion());
			Assert.assertEquals("WinXP", ifcHeader.getOriginatingSystem());
			Assert.assertEquals("dayal", ifcHeader.getAuthorization());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void test3() {
		try {
			IfcHeader ifcHeader = new IfcHeaderParser().parseFileName("('', '2007-04-10T13:03:07', (''), (''), 'IFC Export', 'Esa.Pt', '')", 0);

			Assert.assertEquals("", ifcHeader.getFilename());

			GregorianCalendar gregorianCalendar = new GregorianCalendar();
			gregorianCalendar.set(Calendar.YEAR, 2007);
			gregorianCalendar.set(Calendar.MONTH, 3);
			gregorianCalendar.set(Calendar.DAY_OF_MONTH, 10);
			gregorianCalendar.set(Calendar.HOUR_OF_DAY, 13);
			gregorianCalendar.set(Calendar.MINUTE, 3);
			gregorianCalendar.set(Calendar.SECOND, 7);
			gregorianCalendar.set(Calendar.MILLISECOND, 0);
			
			Assert.assertEquals(gregorianCalendar.getTime(), ifcHeader.getTimeStamp());
			Assert.assertEquals(1, ifcHeader.getAuthor().size());
			Assert.assertEquals("", ifcHeader.getAuthor().get(0));
			Assert.assertEquals(1, ifcHeader.getOrganization().size());
			Assert.assertEquals("", ifcHeader.getOrganization().get(0));
			Assert.assertEquals("IFC Export", ifcHeader.getPreProcessorVersion());
			Assert.assertEquals("Esa.Pt", ifcHeader.getOriginatingSystem());
			Assert.assertEquals("", ifcHeader.getAuthorization());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void test4() {
		try {
			IfcHeader ifcHeader = new IfcHeaderParser().parseFileName("($,'2013-05-02T10:04:35',(''),(''),'Autodesk Revit Architecture 2013','20120221_2030(x64)','')", 0);
			
			Assert.assertEquals(null, ifcHeader.getFilename());

			GregorianCalendar gregorianCalendar = new GregorianCalendar();
			gregorianCalendar.set(Calendar.YEAR, 2013);
			gregorianCalendar.set(Calendar.MONTH, 4);
			gregorianCalendar.set(Calendar.DAY_OF_MONTH, 2);
			gregorianCalendar.set(Calendar.HOUR_OF_DAY, 10);
			gregorianCalendar.set(Calendar.MINUTE, 4);
			gregorianCalendar.set(Calendar.SECOND, 35);
			gregorianCalendar.set(Calendar.MILLISECOND, 0);
			
			Assert.assertEquals(gregorianCalendar.getTime(), ifcHeader.getTimeStamp());
			Assert.assertEquals(1, ifcHeader.getAuthor().size());
			Assert.assertEquals("", ifcHeader.getAuthor().get(0));
			Assert.assertEquals(1, ifcHeader.getOrganization().size());
			Assert.assertEquals("", ifcHeader.getOrganization().get(0));
			Assert.assertEquals("Autodesk Revit Architecture 2013", ifcHeader.getPreProcessorVersion());
			Assert.assertEquals("20120221_2030(x64)", ifcHeader.getOriginatingSystem());
			Assert.assertEquals("", ifcHeader.getAuthorization());
		} catch (DeserializeException e) {
			e.printStackTrace();
			Assert.assertEquals("FILE_NAME.name is not an optional field, but $ used", e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void test5() {
		try {
			IfcHeader ifcHeader = new IfcHeaderParser().parseFileName("('G:\\\\Users\\\\NLST\\\\ArchiCAD\\\\2x.ifc','2006-02-16T17:26:18',('Architect'),('Building Designer Office'),'PreProc - EDM 4.5.0033','Windows System','The authorising person')", 0);
			
			Assert.assertEquals("G:\\Users\\NLST\\ArchiCAD\\2x.ifc", ifcHeader.getFilename());

			GregorianCalendar gregorianCalendar = new GregorianCalendar();
			gregorianCalendar.set(Calendar.YEAR, 2006);
			gregorianCalendar.set(Calendar.MONTH, 1);
			gregorianCalendar.set(Calendar.DAY_OF_MONTH, 16);
			gregorianCalendar.set(Calendar.HOUR_OF_DAY, 17);
			gregorianCalendar.set(Calendar.MINUTE, 26);
			gregorianCalendar.set(Calendar.SECOND, 18);
			gregorianCalendar.set(Calendar.MILLISECOND, 0);
			
			Assert.assertEquals(gregorianCalendar.getTime(), ifcHeader.getTimeStamp());
			Assert.assertEquals(1, ifcHeader.getAuthor().size());
			Assert.assertEquals("Architect", ifcHeader.getAuthor().get(0));
			Assert.assertEquals(1, ifcHeader.getOrganization().size());
			Assert.assertEquals("Building Designer Office", ifcHeader.getOrganization().get(0));
			Assert.assertEquals("PreProc - EDM 4.5.0033", ifcHeader.getPreProcessorVersion());
			Assert.assertEquals("Windows System", ifcHeader.getOriginatingSystem());
			Assert.assertEquals("The authorising person", ifcHeader.getAuthorization());	
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void test6() {
		try {
			IfcHeader ifcHeader = new IfcHeaderParser().parseDescription("(('ArchiCAD 11.00 Release 1 generated IFC file.','Build Number of the Ifc 2x3 interface: 63096 (01-09-2008)\\X\\0A'),'2;1')", 0);

			Assert.assertEquals("ArchiCAD 11.00 Release 1 generated IFC file.", ifcHeader.getDescription().get(0));
			Assert.assertEquals("Build Number of the Ifc 2x3 interface: 63096 (01-09-2008)\n", ifcHeader.getDescription().get(1));
			Assert.assertEquals("2;1", ifcHeader.getImplementationLevel());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void test7() {
		try {
			IfcHeader ifcHeader = new IfcHeaderParser().parseDescription("(('ArchiCAD 11.00 Release 1 generated IFC file.','Build Number of the Ifc 2x3 interface: 63090 (13-06-2008)\\X\\0A'),'2;1')", 0);
			
			Assert.assertEquals("ArchiCAD 11.00 Release 1 generated IFC file.", ifcHeader.getDescription().get(0));
			Assert.assertEquals("Build Number of the Ifc 2x3 interface: 63090 (13-06-2008)\n", ifcHeader.getDescription().get(1));
			Assert.assertEquals("2;1", ifcHeader.getImplementationLevel());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void test8() {
		try {
			IfcHeader ifcHeader = new IfcHeaderParser().parseDescription("((''), '2;1')", 0);

			Assert.assertEquals(1, ifcHeader.getDescription().size());
			Assert.assertEquals("", ifcHeader.getDescription().get(0));
			Assert.assertEquals("2;1", ifcHeader.getImplementationLevel());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void test9() {
		try {
			IfcHeader ifcHeader = new IfcHeaderParser().parseDescription("((), '2;1')", 0);

			Assert.assertEquals(0, ifcHeader.getDescription().size());
			Assert.assertEquals("2;1", ifcHeader.getImplementationLevel());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void test10() {
		try {
			IfcHeader ifcHeader = new IfcHeaderParser().parseFileName(
					"('Y:\\\\IFC\\\\IFC Certification\\\\IFC2x3 ADT Files\\\\Ready for IAI\\\\01-01-03-Clipping-ADT.ifc','2006-12-12T10:07:32',('Autodesk Inc.'),('Autodesk Inc.',''),'AutoCAD Architecture Kiasma Build 17.1.40.0 - 1.0','Microsoft Windows NT 5.1.2600 Service Pack 2','')", 0);
			
			Assert.assertEquals("Y:\\IFC\\IFC Certification\\IFC2x3 ADT Files\\Ready for IAI\\01-01-03-Clipping-ADT.ifc", ifcHeader.getFilename());

			GregorianCalendar gregorianCalendar = new GregorianCalendar();
			gregorianCalendar.set(Calendar.YEAR, 2006);
			gregorianCalendar.set(Calendar.MONTH, 11);
			gregorianCalendar.set(Calendar.DAY_OF_MONTH, 12);
			gregorianCalendar.set(Calendar.HOUR_OF_DAY, 10);
			gregorianCalendar.set(Calendar.MINUTE, 7);
			gregorianCalendar.set(Calendar.SECOND, 32);
			gregorianCalendar.set(Calendar.MILLISECOND, 0);

			Assert.assertEquals(gregorianCalendar.getTime(), ifcHeader.getTimeStamp());
			Assert.assertEquals(1, ifcHeader.getAuthor().size());
			Assert.assertEquals("Autodesk Inc.", ifcHeader.getAuthor().get(0));
			Assert.assertEquals(2, ifcHeader.getOrganization().size());
			Assert.assertEquals("Autodesk Inc.", ifcHeader.getOrganization().get(0));
			Assert.assertEquals("", ifcHeader.getOrganization().get(1));
			Assert.assertEquals("AutoCAD Architecture Kiasma Build 17.1.40.0 - 1.0", ifcHeader.getPreProcessorVersion());
			Assert.assertEquals("Microsoft Windows NT 5.1.2600 Service Pack 2", ifcHeader.getOriginatingSystem());
			Assert.assertEquals("", ifcHeader.getAuthorization());			

		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void test11() {
		try {
			IfcHeader ifcHeader = new IfcHeaderParser().parseFileName(
					"('C:\\\\documents and settings\\\\stephj1\\\\my documents\\\\briefcases\\\\ifc-mbomb\\\\ifc-mbomb_t416\\\\t-block\\\\Views\\\\003-T-Block.dwg','2004-01-26T14:03:27',(''),('Taylor Woodrow'),'IFC-Utility 2x for ADT V. 2, 0, 2, 5   (www.inopso.com) - IFC Toolbox Version 2.x (00/11/07)','Autodesk Architectural Desktop','JS')", 0);
			
			Assert.assertEquals("C:\\documents and settings\\stephj1\\my documents\\briefcases\\ifc-mbomb\\ifc-mbomb_t416\\t-block\\Views\\003-T-Block.dwg", ifcHeader.getFilename());

			GregorianCalendar gregorianCalendar = new GregorianCalendar();
			gregorianCalendar.set(Calendar.YEAR, 2004);
			gregorianCalendar.set(Calendar.MONTH, 0);
			gregorianCalendar.set(Calendar.DAY_OF_MONTH, 26);
			gregorianCalendar.set(Calendar.HOUR_OF_DAY, 14);
			gregorianCalendar.set(Calendar.MINUTE, 3);
			gregorianCalendar.set(Calendar.SECOND, 27);
			gregorianCalendar.set(Calendar.MILLISECOND, 0);

			Assert.assertEquals(gregorianCalendar.getTime(), ifcHeader.getTimeStamp());
			Assert.assertEquals(1, ifcHeader.getAuthor().size());
			Assert.assertEquals("", ifcHeader.getAuthor().get(0));
			Assert.assertEquals(1, ifcHeader.getOrganization().size());
			Assert.assertEquals("Taylor Woodrow", ifcHeader.getOrganization().get(0));
			Assert.assertEquals("IFC-Utility 2x for ADT V. 2, 0, 2, 5   (www.inopso.com) - IFC Toolbox Version 2.x (00/11/07)", ifcHeader.getPreProcessorVersion());
			Assert.assertEquals("Autodesk Architectural Desktop", ifcHeader.getOriginatingSystem());
			Assert.assertEquals("JS", ifcHeader.getAuthorization());			
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void test12() {
		try {
			IfcHeader ifcHeader = new IfcHeaderParser().parseFileName(
					"('C:\\\\IFC\\\\IFC Certification\\\\IFC2x3 ADT Files\\\\Ready for IAI\\\\00-01-01-BasicSpaces-ADT-fix1.ifc','2006-12-14T10:55:37',('Autodesk Inc.'),('Autodesk Inc.',''),'AutoCAD Architecture Kiasma Build 17.1.40.0 - 1.0','Microsoft Windows NT 5.1.2600 Service Pack 2','')", 0);

			Assert.assertEquals("C:\\IFC\\IFC Certification\\IFC2x3 ADT Files\\Ready for IAI\\00-01-01-BasicSpaces-ADT-fix1.ifc", ifcHeader.getFilename());

			GregorianCalendar gregorianCalendar = new GregorianCalendar();
			gregorianCalendar.set(Calendar.YEAR, 2006);
			gregorianCalendar.set(Calendar.MONTH, 11);
			gregorianCalendar.set(Calendar.DAY_OF_MONTH, 14);
			gregorianCalendar.set(Calendar.HOUR_OF_DAY, 10);
			gregorianCalendar.set(Calendar.MINUTE, 55);
			gregorianCalendar.set(Calendar.SECOND, 37);
			gregorianCalendar.set(Calendar.MILLISECOND, 0);

			Assert.assertEquals(gregorianCalendar.getTime(), ifcHeader.getTimeStamp());
			Assert.assertEquals(1, ifcHeader.getAuthor().size());
			Assert.assertEquals("Autodesk Inc.", ifcHeader.getAuthor().get(0));
			Assert.assertEquals(2, ifcHeader.getOrganization().size());
			Assert.assertEquals("Autodesk Inc.", ifcHeader.getOrganization().get(0));
			Assert.assertEquals("", ifcHeader.getOrganization().get(1));
			Assert.assertEquals("AutoCAD Architecture Kiasma Build 17.1.40.0 - 1.0", ifcHeader.getPreProcessorVersion());
			Assert.assertEquals("Microsoft Windows NT 5.1.2600 Service Pack 2", ifcHeader.getOriginatingSystem());
			Assert.assertEquals("", ifcHeader.getAuthorization());			
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void test13() {
		try {
			IfcHeader ifcHeader = new IfcHeaderParser().parseFileName("('WallIFCexport_situationzelfdeguids.ifc','2013-06-27T20:05:58',(''),(''),'Autodesk Revit 2013','20121003_2115(x64) - Exporter 2.7.0.0 - Alternate UI 1.7.0.0',$)", 0);
			
			Assert.assertEquals("WallIFCexport_situationzelfdeguids.ifc", ifcHeader.getFilename());

			GregorianCalendar gregorianCalendar = new GregorianCalendar();
			gregorianCalendar.set(Calendar.YEAR, 2013);
			gregorianCalendar.set(Calendar.MONTH, 5);
			gregorianCalendar.set(Calendar.DAY_OF_MONTH, 27);
			gregorianCalendar.set(Calendar.HOUR_OF_DAY, 20);
			gregorianCalendar.set(Calendar.MINUTE, 05);
			gregorianCalendar.set(Calendar.SECOND, 58);
			gregorianCalendar.set(Calendar.MILLISECOND, 0);

			Assert.assertEquals(gregorianCalendar.getTime(), ifcHeader.getTimeStamp());
			Assert.assertEquals(1, ifcHeader.getAuthor().size());
			Assert.assertEquals("", ifcHeader.getAuthor().get(0));
			Assert.assertEquals(1, ifcHeader.getOrganization().size());
			Assert.assertEquals("", ifcHeader.getOrganization().get(0));
			Assert.assertEquals("Autodesk Revit 2013", ifcHeader.getPreProcessorVersion());
			Assert.assertEquals("20121003_2115(x64) - Exporter 2.7.0.0 - Alternate UI 1.7.0.0", ifcHeader.getOriginatingSystem());
			Assert.assertEquals(null, ifcHeader.getAuthorization());			
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void test14() {
		try {
			IfcHeader ifcHeader = new IfcHeaderParser().parseDescription("(('ArchiCAD 11.00 Release 1 generated IFC file.','Build Number of the Ifc 2x3 interface: 63090 (13-06-2008)\\X\\0A'),'2;1')", 0);
			
			Assert.assertEquals("ArchiCAD 11.00 Release 1 generated IFC file.", ifcHeader.getDescription().get(0));
			Assert.assertEquals("Build Number of the Ifc 2x3 interface: 63090 (13-06-2008)\n", ifcHeader.getDescription().get(1));
			Assert.assertEquals("2;1", ifcHeader.getImplementationLevel());
			
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void test15() {
		try {
			IfcHeader ifcHeader = new IfcHeaderParser().parseFileName(
					"('S:\\\\[IFC]\\\\COMPLETE-BUILDINGS\\\\FZK-MODELS\\\\Buerogebaeude-Zones\\\\ArchiCAD-11\\\\Institute-Var-2\\\\IFC2x3\\\\AC11-Institute-Var-2-IFC.ifc','2008-07-03T15:22:43',('Architect'),('Building Designer Office'),'PreProc - EDM 4.5.0033','Windows System','The authorising person')", 0);
			
			Assert.assertEquals("S:\\[IFC]\\COMPLETE-BUILDINGS\\FZK-MODELS\\Buerogebaeude-Zones\\ArchiCAD-11\\Institute-Var-2\\IFC2x3\\AC11-Institute-Var-2-IFC.ifc", ifcHeader.getFilename());

			GregorianCalendar gregorianCalendar = new GregorianCalendar();
			gregorianCalendar.set(Calendar.YEAR, 2008);
			gregorianCalendar.set(Calendar.MONTH, 6);
			gregorianCalendar.set(Calendar.DAY_OF_MONTH, 3);
			gregorianCalendar.set(Calendar.HOUR_OF_DAY, 15);
			gregorianCalendar.set(Calendar.MINUTE, 22);
			gregorianCalendar.set(Calendar.SECOND, 43);
			gregorianCalendar.set(Calendar.MILLISECOND, 0);

			Assert.assertEquals(gregorianCalendar.getTime(), ifcHeader.getTimeStamp());
			Assert.assertEquals(1, ifcHeader.getAuthor().size());
			Assert.assertEquals("Architect", ifcHeader.getAuthor().get(0));
			Assert.assertEquals(1, ifcHeader.getOrganization().size());
			Assert.assertEquals("Building Designer Office", ifcHeader.getOrganization().get(0));
			Assert.assertEquals("PreProc - EDM 4.5.0033", ifcHeader.getPreProcessorVersion());
			Assert.assertEquals("Windows System", ifcHeader.getOriginatingSystem());
			Assert.assertEquals("The authorising person", ifcHeader.getAuthorization());			
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void test16() {
		try {
			IfcHeader ifcHeader = new IfcHeaderParser().parseFileName(
					"('V:\\\\R\\S\\E\\S\\/zn\\S\\C\\S\\)\\\\Proteo\\\\Nuselsk\\S\\C\\S\\= most BIM.14003\\\\Pracovn\\S\\C\\S\\-\\\\Martin\\\\IFC test\\\\6\\\\mal\\S\\C\\S\\= model - fid jako ifc tag NAME.ifc','2014-10-13T12:28:40',('Architect'),('Building Designer Office'),'PreProc - EDM 5.0','IFC file generated by Graphisoft ArchiCAD-64 17.0.0 CZE FULL Windows version (IFC2x3 add-on version: 6004 CZE FULL).','The authorising person')", 0);
			
			Assert.assertEquals("V:\\RÅ¯znÃ©\\Proteo\\NuselskÃ½ most BIM.14003\\PracovnÃ­\\Martin\\IFC test\\6\\malÃ½ model - fid jako ifc tag NAME.ifc", ifcHeader.getFilename());

			GregorianCalendar gregorianCalendar = new GregorianCalendar();
			gregorianCalendar.set(Calendar.YEAR, 2014);
			gregorianCalendar.set(Calendar.MONTH, 9);
			gregorianCalendar.set(Calendar.DAY_OF_MONTH, 13);
			gregorianCalendar.set(Calendar.HOUR_OF_DAY, 12);
			gregorianCalendar.set(Calendar.MINUTE, 28);
			gregorianCalendar.set(Calendar.SECOND, 40);
			gregorianCalendar.set(Calendar.MILLISECOND, 0);

			Assert.assertEquals(gregorianCalendar.getTime(), ifcHeader.getTimeStamp());
			Assert.assertEquals(1, ifcHeader.getAuthor().size());
			Assert.assertEquals("Architect", ifcHeader.getAuthor().get(0));
			Assert.assertEquals(1, ifcHeader.getOrganization().size());
			Assert.assertEquals("Building Designer Office", ifcHeader.getOrganization().get(0));
			Assert.assertEquals("PreProc - EDM 5.0", ifcHeader.getPreProcessorVersion());
			Assert.assertEquals("IFC file generated by Graphisoft ArchiCAD-64 17.0.0 CZE FULL Windows version (IFC2x3 add-on version: 6004 CZE FULL).", ifcHeader.getOriginatingSystem());
			Assert.assertEquals("The authorising person", ifcHeader.getAuthorization());			
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void test17() {
		try {
			IfcHeader ifcHeader = new IfcHeaderParser().parseFileName(
					"('/Volumes/KIM-HD-001/Projects/IAI/\\S\\c\\X\\82\\S\\$\\S\\c\\X\\83\\S\\3\\S\\c\\X\\83\\X\\95\\S\\c\\X\\82\\X\\9A\\S\\c\\X\\83\\S\\*\\S\\e\\X\\88\\X\\86\\S\\g\\S\\'\\X\\91\\S\\d\\S\\<\\X\\9A/20130419_\\S\\f\\X\\96\\X\\87\\S\\e\\S\\-\\X\\97\\S\\c\\X\\82\\S\\3\\S\\c\\X\\83\\S\\<\\S\\c\\X\\83\\X\\88\\S\\c\\X\\82\\X\\99\\S\\c\\X\\82\\S\\5\\S\\c\\X\\83\\S\\3\\S\\c\\X\\83\\X\\95\\S\\c\\X\\82\\X\\9A\\S\\c\\X\\83\\S\\+/AC16_sjis-VW-sjis.ifc','2013-04-19T12:38:54',('Architect'),('Nemetschek Vectorworks, Inc.'),'Vectorworks Architect 2013 SP3 (Build 183378) by Nemetschek Vectorworks, Inc.','Macintosh','')", 0);
			
			// This looks like a very weird filename, other packages also seem to struggle with it
			Assert.assertEquals("/Volumes/KIM-HD-001/Projects/IAI/ã¤ã³ãããªåç§ä¼/20130419_æå­ã³ã¼ãããµã³ããã«/AC16_sjis-VW-sjis.ifc", ifcHeader.getFilename());

			GregorianCalendar gregorianCalendar = new GregorianCalendar();
			gregorianCalendar.set(Calendar.YEAR, 2013);
			gregorianCalendar.set(Calendar.MONTH, 3);
			gregorianCalendar.set(Calendar.DAY_OF_MONTH, 19);
			gregorianCalendar.set(Calendar.HOUR_OF_DAY, 12);
			gregorianCalendar.set(Calendar.MINUTE, 38);
			gregorianCalendar.set(Calendar.SECOND, 54);
			gregorianCalendar.set(Calendar.MILLISECOND, 0);

			Assert.assertEquals(gregorianCalendar.getTime(), ifcHeader.getTimeStamp());
			Assert.assertEquals(1, ifcHeader.getAuthor().size());
			Assert.assertEquals("Architect", ifcHeader.getAuthor().get(0));
			Assert.assertEquals(1, ifcHeader.getOrganization().size());
			Assert.assertEquals("Nemetschek Vectorworks, Inc.", ifcHeader.getOrganization().get(0));
			Assert.assertEquals("Vectorworks Architect 2013 SP3 (Build 183378) by Nemetschek Vectorworks, Inc.", ifcHeader.getPreProcessorVersion());
			Assert.assertEquals("Macintosh", ifcHeader.getOriginatingSystem());
			Assert.assertEquals("", ifcHeader.getAuthorization());			
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void test18() {
		try {
			IfcHeader ifcHeader = new IfcHeaderParser().parseDescription("(('ViewDefinition [CoordinationView, QuantityTakeOffAddOnView]','Option [Visible elements;Keep GUIDs;All Domain;All elements;All struct.;scale:100.000000;CSG:Extruded;Comp. wall:Extruded;Building element parts;Chained beam:Extruded;Comp. slab:Extruded]'),'2;1')", 0);
			
			Assert.assertEquals("ViewDefinition [CoordinationView, QuantityTakeOffAddOnView]", ifcHeader.getDescription().get(0));
			Assert.assertEquals("Option [Visible elements;Keep GUIDs;All Domain;All elements;All struct.;scale:100.000000;CSG:Extruded;Comp. wall:Extruded;Building element parts;Chained beam:Extruded;Comp. slab:Extruded]", ifcHeader.getDescription().get(1));
			Assert.assertEquals("2;1", ifcHeader.getImplementationLevel());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void test21() {
		try {
			IfcHeader ifcHeader = new IfcHeaderParser().parseFileName("('TBlockArchicadDucts',\r\n     '2004-01-22T20:08:03',\r\n          ('sdai-user'),\r\n          ('ANONYMOUS ORGANISATION'),\r\n          'EXPRESS Data Manager version 20030909',   \r\n       $,\r\n          $)", 0);
			
			Assert.assertEquals("TBlockArchicadDucts", ifcHeader.getFilename());

			GregorianCalendar gregorianCalendar = new GregorianCalendar();
			gregorianCalendar.set(Calendar.YEAR, 2004);
			gregorianCalendar.set(Calendar.MONTH, 0);
			gregorianCalendar.set(Calendar.DAY_OF_MONTH, 22);
			gregorianCalendar.set(Calendar.HOUR_OF_DAY, 20);
			gregorianCalendar.set(Calendar.MINUTE, 8);
			gregorianCalendar.set(Calendar.SECOND, 3);
			gregorianCalendar.set(Calendar.MILLISECOND, 0);

			Assert.assertEquals(gregorianCalendar.getTime(), ifcHeader.getTimeStamp());
			Assert.assertEquals(1, ifcHeader.getAuthor().size());
			Assert.assertEquals("sdai-user", ifcHeader.getAuthor().get(0));
			Assert.assertEquals(1, ifcHeader.getOrganization().size());
			Assert.assertEquals("ANONYMOUS ORGANISATION", ifcHeader.getOrganization().get(0));
			Assert.assertEquals("EXPRESS Data Manager version 20030909", ifcHeader.getPreProcessorVersion());
			Assert.assertEquals(null, ifcHeader.getOriginatingSystem());
			Assert.assertEquals(null, ifcHeader.getAuthorization());			
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}
}