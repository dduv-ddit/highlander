/*****************************************************************************************
*
* Highlander - Copyright (C) <2012-2020> <Université catholique de Louvain (UCLouvain)>
* 	
* List of the contributors to the development of Highlander: see LICENSE file.
* Description and complete License: see LICENSE file.
* 	
* This program (Highlander) is free software: 
* you can redistribute it and/or modify it under the terms of the 
* GNU General Public License as published by the Free Software Foundation, 
* either version 3 of the License, or (at your option) any later version.
* 
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with this program (see COPYING file).  If not, 
* see <http://www.gnu.org/licenses/>.
* 
*****************************************************************************************/

/**
*
* @author Raphael Helaers
*
*/

package be.uclouvain.ngs.highlander;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;

public class Resources {

	public final static Color rowHeadBackground = new Color(51,102,153);
	public final static Color rowHeadForeground = Color.WHITE;
	
	public enum Palette {
		Amber,
		Blue,
		BlueGray,
		Brown,
		Cyan,
		DeepOrange,
		DeepPurple,
		Gray,
		Green,
		Indigo,
		LightBlue,
		LightGreen,
		Lime,
		Orange,
		Pink,
		Purple,
		Red,
		Teal,
		Yellow,
	}
	
	/**
	 * Materia palette
	 * 
	 * These color palettes, originally created by Material Design in 2014, 
	 * are comprised of colors designed to work together harmoniously.
	 * 
	 * Intensity of 500 means the primary color. 
	 * Higher values, like 900, mean "darker" versions of the primary color. 
	 * Correspondingly, lower values means "lighter" versions of the primary color, like 50.
	 * 
	 * The accent colors (Axxx) should be used for the floating action button and interactive elements, such as:
	 * - Text fields and cursors
	 * - Text selection
	 * - Progress bars
	 * - Selection controls, buttons, and sliders Links
	 * 
	 * See https://material.io/design/color/the-color-system.html#tools-for-picking-colors for the palette
	 * 
	 * @param color a color to chose in the Palette enumeration
	 * @param intensity a value between 0 and 1000 (primary color is 500, lower is lighter, higher is darker)
	 * @param accent true to get accent color
	 * @return the color
	 */
	public static Color getColor(Palette color, int intensity, boolean accent){
		switch(color) {
		case Red:
			if (accent) {
				if (intensity <= 100) return Color.decode("#FF8A80");
				if (intensity <= 200) return Color.decode("#FF5252");
				if (intensity <= 400) return Color.decode("#FF1744");
				else return Color.decode("#D50000");
			} else {
				if (intensity <= 50) return Color.decode("#FFEBEE");
				if (intensity <= 100) return Color.decode("#FFCDD2");
				if (intensity <= 200) return Color.decode("#EF9A9A");
				if (intensity <= 300) return Color.decode("#E57373");
				if (intensity <= 400) return Color.decode("#EF5350");
				if (intensity <= 500) return Color.decode("#F44336");
				if (intensity <= 600) return Color.decode("#E53935");
				if (intensity <= 700) return Color.decode("#D32F2F");
				if (intensity <= 800) return Color.decode("#C62828");
				else  return Color.decode("#B71C1C");
			}
		case Pink:
			if (accent) {
				if (intensity <= 100) return Color.decode("#FF80AB");
				if (intensity <= 200) return Color.decode("#FF4081");
				if (intensity <= 400) return Color.decode("#F50057");
				else return Color.decode("#C51162");
			} else {
				if (intensity <= 50) return Color.decode("#FCE4EC");
				if (intensity <= 100) return Color.decode("#F8BBD0");
				if (intensity <= 200) return Color.decode("#F48FB1");
				if (intensity <= 300) return Color.decode("#F06292");
				if (intensity <= 400) return Color.decode("#EC407A");
				if (intensity <= 500) return Color.decode("#E91E63");
				if (intensity <= 600) return Color.decode("#D81B60");
				if (intensity <= 700) return Color.decode("#C2185B");
				if (intensity <= 800) return Color.decode("#AD1457");
				else  return Color.decode("#880E4F");
			}
		case Purple:
			if (accent) {
				if (intensity <= 100) return Color.decode("#EA80FC");
				if (intensity <= 200) return Color.decode("#E040FB");
				if (intensity <= 400) return Color.decode("#D500F9");
				else return Color.decode("#AA00FF");
			} else {
				if (intensity <= 50) return Color.decode("#F3E5F5");
				if (intensity <= 100) return Color.decode("#E1BEE7");
				if (intensity <= 200) return Color.decode("#CE93D8");
				if (intensity <= 300) return Color.decode("#BA68C8");
				if (intensity <= 400) return Color.decode("#AB47BC");
				if (intensity <= 500) return Color.decode("#9C27B0");
				if (intensity <= 600) return Color.decode("#8E24AA");
				if (intensity <= 700) return Color.decode("#7B1FA2");
				if (intensity <= 800) return Color.decode("#6A1B9A");
				else  return Color.decode("#4A148C");
			}
		case DeepPurple:
			if (accent) {
				if (intensity <= 100) return Color.decode("#B388FF");
				if (intensity <= 200) return Color.decode("#7C4DFF");
				if (intensity <= 400) return Color.decode("#651FFF");
				else return Color.decode("#6200EA");
			} else {
				if (intensity <= 50) return Color.decode("#EDE7F6");
				if (intensity <= 100) return Color.decode("#D1C4E9");
				if (intensity <= 200) return Color.decode("#B39DDB");
				if (intensity <= 300) return Color.decode("#9575CD");
				if (intensity <= 400) return Color.decode("#7E57C2");
				if (intensity <= 500) return Color.decode("#673AB7");
				if (intensity <= 600) return Color.decode("#5E35B1");
				if (intensity <= 700) return Color.decode("#512DA8");
				if (intensity <= 800) return Color.decode("#4527A0");
				else  return Color.decode("#311B92");
			}
		case Indigo:
			if (accent) {
				if (intensity <= 100) return Color.decode("#8C9EFF");
				if (intensity <= 200) return Color.decode("#536DFE");
				if (intensity <= 400) return Color.decode("#3D5AFE");
				else return Color.decode("#304FFE");
			} else {
				if (intensity <= 50) return Color.decode("#E8EAF6");
				if (intensity <= 100) return Color.decode("#C5CAE9");
				if (intensity <= 200) return Color.decode("#9FA8DA");
				if (intensity <= 300) return Color.decode("#7986CB");
				if (intensity <= 400) return Color.decode("#5C6BC0");
				if (intensity <= 500) return Color.decode("#3F51B5");
				if (intensity <= 600) return Color.decode("#3949AB");
				if (intensity <= 700) return Color.decode("#303F9F");
				if (intensity <= 800) return Color.decode("#283593");
				else  return Color.decode("#1A237E");
			}
		case Blue:
			if (accent) {
				if (intensity <= 100) return Color.decode("#82B1FF");
				if (intensity <= 200) return Color.decode("#448AFF");
				if (intensity <= 400) return Color.decode("#2979FF");
				else return Color.decode("#2962FF");
			} else {
				if (intensity <= 50) return Color.decode("#E3F2FD");
				if (intensity <= 100) return Color.decode("#BBDEFB");
				if (intensity <= 200) return Color.decode("#90CAF9");
				if (intensity <= 300) return Color.decode("#64B5F6");
				if (intensity <= 400) return Color.decode("#42A5F5");
				if (intensity <= 500) return Color.decode("#2196F3");
				if (intensity <= 600) return Color.decode("#1E88E5");
				if (intensity <= 700) return Color.decode("#1976D2");
				if (intensity <= 800) return Color.decode("#1565C0");
				else  return Color.decode("#0D47A1");
			}
		case LightBlue:
			if (accent) {
				if (intensity <= 100) return Color.decode("#80D8FF");
				if (intensity <= 200) return Color.decode("#40C4FF");
				if (intensity <= 400) return Color.decode("#00B0FF");
				else return Color.decode("#0091EA");
			} else {
				if (intensity <= 50) return Color.decode("#E1F5FE");
				if (intensity <= 100) return Color.decode("#B3E5FC");
				if (intensity <= 200) return Color.decode("#81D4FA");
				if (intensity <= 300) return Color.decode("#4FC3F7");
				if (intensity <= 400) return Color.decode("#29B6F6");
				if (intensity <= 500) return Color.decode("#03A9F4");
				if (intensity <= 600) return Color.decode("#039BE5");
				if (intensity <= 700) return Color.decode("#0288D1");
				if (intensity <= 800) return Color.decode("#0277BD");
				else  return Color.decode("#01579B");
			}
		case Cyan:
			if (accent) {
				if (intensity <= 100) return Color.decode("#84FFFF");
				if (intensity <= 200) return Color.decode("#18FFFF");
				if (intensity <= 400) return Color.decode("#00E5FF");
				else return Color.decode("#00B8D4");
			} else {
				if (intensity <= 50) return Color.decode("#E0F7FA");
				if (intensity <= 100) return Color.decode("#B2EBF2");
				if (intensity <= 200) return Color.decode("#80DEEA");
				if (intensity <= 300) return Color.decode("#4DD0E1");
				if (intensity <= 400) return Color.decode("#26C6DA");
				if (intensity <= 500) return Color.decode("#00BCD4");
				if (intensity <= 600) return Color.decode("#00ACC1");
				if (intensity <= 700) return Color.decode("#0097A7");
				if (intensity <= 800) return Color.decode("#00838F");
				else  return Color.decode("#006064");
			}
		case Teal:
			if (accent) {
				if (intensity <= 100) return Color.decode("#A7FFEB");
				if (intensity <= 200) return Color.decode("#64FFDA");
				if (intensity <= 400) return Color.decode("#1DE9B6");
				else return Color.decode("#00BFA5");
			} else {
				if (intensity <= 50) return Color.decode("#E0F2F1");
				if (intensity <= 100) return Color.decode("#B2DFDB");
				if (intensity <= 200) return Color.decode("#80CBC4");
				if (intensity <= 300) return Color.decode("#4DB6AC");
				if (intensity <= 400) return Color.decode("#26A69A");
				if (intensity <= 500) return Color.decode("#009688");
				if (intensity <= 600) return Color.decode("#00897B");
				if (intensity <= 700) return Color.decode("#00796B");
				if (intensity <= 800) return Color.decode("#00695C");
				else  return Color.decode("#004D40");
			}
		case Green:
			if (accent) {
				if (intensity <= 100) return Color.decode("#B9F6CA");
				if (intensity <= 200) return Color.decode("#69F0AE");
				if (intensity <= 400) return Color.decode("#00E676");
				else return Color.decode("#00C853");
			} else {
				if (intensity <= 50) return Color.decode("#E8F5E9");
				if (intensity <= 100) return Color.decode("#C8E6C9");
				if (intensity <= 200) return Color.decode("#A5D6A7");
				if (intensity <= 300) return Color.decode("#81C784");
				if (intensity <= 400) return Color.decode("#66BB6A");
				if (intensity <= 500) return Color.decode("#4CAF50");
				if (intensity <= 600) return Color.decode("#43A047");
				if (intensity <= 700) return Color.decode("#388E3C");
				if (intensity <= 800) return Color.decode("#2E7D32");
				else  return Color.decode("#1B5E20");
			}
		case LightGreen:
			if (accent) {
				if (intensity <= 100) return Color.decode("#CCFF90");
				if (intensity <= 200) return Color.decode("#B2FF59");
				if (intensity <= 400) return Color.decode("#76FF03");
				else return Color.decode("#64DD17");
			} else {
				if (intensity <= 50) return Color.decode("#F1F8E9");
				if (intensity <= 100) return Color.decode("#DCEDC8");
				if (intensity <= 200) return Color.decode("#C5E1A5");
				if (intensity <= 300) return Color.decode("#AED581");
				if (intensity <= 400) return Color.decode("#9CCC65");
				if (intensity <= 500) return Color.decode("#8BC34A");
				if (intensity <= 600) return Color.decode("#7CB342");
				if (intensity <= 700) return Color.decode("#689F38");
				if (intensity <= 800) return Color.decode("#558B2F");
				else  return Color.decode("#33691E");
			}
		case Lime:
			if (accent) {
				if (intensity <= 100) return Color.decode("#F4FF81");
				if (intensity <= 200) return Color.decode("#EEFF41");
				if (intensity <= 400) return Color.decode("#C6FF00");
				else return Color.decode("#AEEA00");
			} else {
				if (intensity <= 50) return Color.decode("#F9FBE7");
				if (intensity <= 100) return Color.decode("#F0F4C3");
				if (intensity <= 200) return Color.decode("#E6EE9C");
				if (intensity <= 300) return Color.decode("#DCE775");
				if (intensity <= 400) return Color.decode("#D4E157");
				if (intensity <= 500) return Color.decode("#CDDC39");
				if (intensity <= 600) return Color.decode("#C0CA33");
				if (intensity <= 700) return Color.decode("#AFB42B");
				if (intensity <= 800) return Color.decode("#9E9D24");
				else  return Color.decode("#827717");
			}
		case Yellow:
			if (accent) {
				if (intensity <= 100) return Color.decode("#FFFF8D");
				if (intensity <= 200) return Color.decode("#FFFF00");
				if (intensity <= 400) return Color.decode("#FFEA00");
				else return Color.decode("#FFD600");
			} else {
				if (intensity <= 50) return Color.decode("#FFFDE7");
				if (intensity <= 100) return Color.decode("#FFF9C4");
				if (intensity <= 200) return Color.decode("#FFF59D");
				if (intensity <= 300) return Color.decode("#FFF176");
				if (intensity <= 400) return Color.decode("#FFEE58");
				if (intensity <= 500) return Color.decode("#FFEB3B");
				if (intensity <= 600) return Color.decode("#FDD835");
				if (intensity <= 700) return Color.decode("#FBC02D");
				if (intensity <= 800) return Color.decode("#F9A825");
				else  return Color.decode("#F57F17");
			}
		case Amber:
			if (accent) {
				if (intensity <= 100) return Color.decode("#FFE57F");
				if (intensity <= 200) return Color.decode("#FFD740");
				if (intensity <= 400) return Color.decode("#FFC400");
				else return Color.decode("#FFAB00");
			} else {
				if (intensity <= 50) return Color.decode("#FFF8E1");
				if (intensity <= 100) return Color.decode("#FFECB3");
				if (intensity <= 200) return Color.decode("#FFE082");
				if (intensity <= 300) return Color.decode("#FFD54F");
				if (intensity <= 400) return Color.decode("#FFCA28");
				if (intensity <= 500) return Color.decode("#FFC107");
				if (intensity <= 600) return Color.decode("#FFB300");
				if (intensity <= 700) return Color.decode("#FFA000");
				if (intensity <= 800) return Color.decode("#FF8F00");
				else  return Color.decode("#FF6F00");
			}
		case Orange:
			if (accent) {
				if (intensity <= 100) return Color.decode("#FFD180");
				if (intensity <= 200) return Color.decode("#FFAB40");
				if (intensity <= 400) return Color.decode("#FF9100");
				else return Color.decode("#FF6D00");
			} else {
				if (intensity <= 50) return Color.decode("#FFF3E0");
				if (intensity <= 100) return Color.decode("#FFE0B2");
				if (intensity <= 200) return Color.decode("#FFCC80");
				if (intensity <= 300) return Color.decode("#FFB74D");
				if (intensity <= 400) return Color.decode("#FFA726");
				if (intensity <= 500) return Color.decode("#FF9800");
				if (intensity <= 600) return Color.decode("#FB8C00");
				if (intensity <= 700) return Color.decode("#F57C00");
				if (intensity <= 800) return Color.decode("#EF6C00");
				else  return Color.decode("#E65100");
			}
		case DeepOrange:
			if (accent) {
				if (intensity <= 100) return Color.decode("#FF9E80");
				if (intensity <= 200) return Color.decode("#FF6E40");
				if (intensity <= 400) return Color.decode("#FF3D00");
				else return Color.decode("#DD2C00");
			} else {
				if (intensity <= 50) return Color.decode("#FBE9E7");
				if (intensity <= 100) return Color.decode("#FFCCBC");
				if (intensity <= 200) return Color.decode("#FFAB91");
				if (intensity <= 300) return Color.decode("#FF8A65");
				if (intensity <= 400) return Color.decode("#FF7043");
				if (intensity <= 500) return Color.decode("#FF5722");
				if (intensity <= 600) return Color.decode("#F4511E");
				if (intensity <= 700) return Color.decode("#E64A19");
				if (intensity <= 800) return Color.decode("#D84315");
				else  return Color.decode("#BF360C");
			}
		case Brown:
			if (intensity <= 50) return Color.decode("#EFEBE9");
			if (intensity <= 100) return Color.decode("#D7CCC8");
			if (intensity <= 200) return Color.decode("#BCAAA4");
			if (intensity <= 300) return Color.decode("#A1887F");
			if (intensity <= 400) return Color.decode("#8D6E63");
			if (intensity <= 500) return Color.decode("#795548");
			if (intensity <= 600) return Color.decode("#6D4C41");
			if (intensity <= 700) return Color.decode("#5D4037");
			if (intensity <= 800) return Color.decode("#4E342E");
			else  return Color.decode("#3E2723");
		case Gray:
			if (intensity <= 50) return Color.decode("#FAFAFA");
			if (intensity <= 100) return Color.decode("#F5F5F5");
			if (intensity <= 200) return Color.decode("#EEEEEE");
			if (intensity <= 300) return Color.decode("#E0E0E0");
			if (intensity <= 400) return Color.decode("#BDBDBD");
			if (intensity <= 500) return Color.decode("#9E9E9E");
			if (intensity <= 600) return Color.decode("#757575");
			if (intensity <= 700) return Color.decode("#616161");
			if (intensity <= 800) return Color.decode("#424242");
			else  return Color.decode("#212121");
		case BlueGray:
			if (intensity <= 50) return Color.decode("#ECEFF1");
			if (intensity <= 100) return Color.decode("#CFD8DC");
			if (intensity <= 200) return Color.decode("#B0BEC5");
			if (intensity <= 300) return Color.decode("#90A4AE");
			if (intensity <= 400) return Color.decode("#78909C");
			if (intensity <= 500) return Color.decode("#607D8B");
			if (intensity <= 600) return Color.decode("#546E7A");
			if (intensity <= 700) return Color.decode("#455A64");
			if (intensity <= 800) return Color.decode("#37474F");
			else  return Color.decode("#263238");
		default:
			return Color.WHITE;
		}
	}
	
	public static Color getTableEvenRowBackgroundColor(Palette color){
		return getColor(color, 100, false);
	}
	
	public static Color getTableOddRowBackgroundColor(Palette color){
		return getColor(color, 50, false);
	}
	
  public final static ImageIcon iHighlander= new ImageIcon(Highlander.class.getResource("resources/highlander.png"));
  public final static ImageIcon iAdminstrationTools= new ImageIcon(Highlander.class.getResource("resources/administration_tools.png"));
  public final static ImageIcon iIonImporter= new ImageIcon(Highlander.class.getResource("resources/analysis_iontorrent.png"));
  public final static ImageIcon iProjectManager= new ImageIcon(Highlander.class.getResource("resources/project_manager.png"));
  public final static ImageIcon iDbPatcher= new ImageIcon(Highlander.class.getResource("resources/db_patcher.png"));

  //Waiting animation
  public final static ImageIcon iWait0 = new ImageIcon(Highlander.class.getResource("resources/waiting_0.png"));
  public final static ImageIcon iWait1 = new ImageIcon(Highlander.class.getResource("resources/waiting_1.png"));
  public final static ImageIcon iWait2 = new ImageIcon(Highlander.class.getResource("resources/waiting_2.png"));
  public final static ImageIcon iWait3 = new ImageIcon(Highlander.class.getResource("resources/waiting_3.png"));
  public final static ImageIcon iWait4 = new ImageIcon(Highlander.class.getResource("resources/waiting_4.png"));
  public final static ImageIcon iWait5 = new ImageIcon(Highlander.class.getResource("resources/waiting_5.png"));
  public final static ImageIcon iWait6 = new ImageIcon(Highlander.class.getResource("resources/waiting_6.png"));
  public final static ImageIcon iWait7 = new ImageIcon(Highlander.class.getResource("resources/waiting_7.png"));
  public final static ImageIcon iWait8 = new ImageIcon(Highlander.class.getResource("resources/waiting_8.png"));
  public final static ImageIcon iLoading = new ImageIcon(Highlander.class.getResource("resources/loading.png"));
  
  //General buttons
  public final static ImageIcon iButtonApply = new ImageIcon(Highlander.class.getResource("resources/checked.png"));
  public final static ImageIcon iCross = new ImageIcon(Highlander.class.getResource("resources/cross.png"));
  public final static ImageIcon iAttention = new ImageIcon(Highlander.class.getResource("resources/attention.png"));
  public final static ImageIcon iQuestion = new ImageIcon(Highlander.class.getResource("resources/question.png"));
  public final static ImageIcon iRoman1 = new ImageIcon(Highlander.class.getResource("resources/roman_1.png"));
  public final static ImageIcon iRoman2 = new ImageIcon(Highlander.class.getResource("resources/roman_2.png"));
  public final static ImageIcon iRoman3 = new ImageIcon(Highlander.class.getResource("resources/roman_3.png"));
  public final static ImageIcon iRoman4 = new ImageIcon(Highlander.class.getResource("resources/roman_4.png"));
  public final static ImageIcon iRoman5 = new ImageIcon(Highlander.class.getResource("resources/roman_5.png"));
  public final static ImageIcon iExit = new ImageIcon(Highlander.class.getResource("resources/exit.png"));
  public final static ImageIcon iSave = new ImageIcon(Highlander.class.getResource("resources/save.png"));
  public final static ImageIcon iLoad = new ImageIcon(Highlander.class.getResource("resources/load.png"));
  public final static ImageIcon iExportFile = new ImageIcon(Highlander.class.getResource("resources/export_file.png"));
  public final static ImageIcon iImportFile = new ImageIcon(Highlander.class.getResource("resources/folder-green.png"));
  public final static ImageIcon iExportJpeg = new ImageIcon(Highlander.class.getResource("resources/export_jpeg.png"));
  public final static ImageIcon iSortAZ = new ImageIcon(Highlander.class.getResource("resources/sort_AZ.png"));
  public final static ImageIcon iEditWrench = new ImageIcon(Highlander.class.getResource("resources/edit_wrench.png"));
  public final static ImageIcon iEditPen = new ImageIcon(Highlander.class.getResource("resources/edit_pen.png"));
  public final static ImageIcon iPrinter = new ImageIcon(Highlander.class.getResource("resources/printer.png"));
  public final static ImageIcon iCopy = new ImageIcon(Highlander.class.getResource("resources/edit-copy.png"));
  public final static ImageIcon iPin = new ImageIcon(Highlander.class.getResource("resources/pin.png"));
  public final static ImageIcon iUnpin = new ImageIcon(Highlander.class.getResource("resources/unpin.png"));
  public final static ImageIcon iReset = new ImageIcon(Highlander.class.getResource("resources/reset.png"));

  //Main add/remove button
  public final static ImageIcon i3dPlus = new ImageIcon(Highlander.class.getResource("resources/pm_3d_plus.png"));
  public final static ImageIcon i3dMinus = new ImageIcon(Highlander.class.getResource("resources/pm_3d_minus.png"));
  //Secondary add/remove button (when the main one is already in use in the frame, like magic filters)
  public final static ImageIcon iFaintPlus = new ImageIcon(Highlander.class.getResource("resources/pm_faint_plus.png"));
  public final static ImageIcon iFaintMinus = new ImageIcon(Highlander.class.getResource("resources/pm_faint_minus.png"));
  //Expand/collapse button (e.g. details boxes)
  public final static ImageIcon i2dPlus = new ImageIcon(Highlander.class.getResource("resources/pm_2d_plus.png"));
  public final static ImageIcon i2dMinus = new ImageIcon(Highlander.class.getResource("resources/pm_2d_minus.png"));
  //Unused plus/minus
  public final static ImageIcon iIsometricPlus = new ImageIcon(Highlander.class.getResource("resources/pm_isometric_plus.png"));
  public final static ImageIcon iIsometricMinus = new ImageIcon(Highlander.class.getResource("resources/pm_isometric_minus.png"));

  //Arrows icons
  public final static ImageIcon iArrowRight = new ImageIcon(Highlander.class.getResource("resources/arrow-right.png"));
  public final static ImageIcon iArrowLeft = new ImageIcon(Highlander.class.getResource("resources/arrow-left.png"));
  public final static ImageIcon iArrowDoubleRight = new ImageIcon(Highlander.class.getResource("resources/arrow-right-double.png"));
  public final static ImageIcon iArrowDoubleLeft = new ImageIcon(Highlander.class.getResource("resources/arrow-left-double.png"));
  public final static ImageIcon iArrowDoubleUp = new ImageIcon(Highlander.class.getResource("resources/arrow-up-double.png"));
  public final static ImageIcon iArrowDoubleDown = new ImageIcon(Highlander.class.getResource("resources/arrow-down-double.png"));

  //Tree icons
  public final static ImageIcon iTreeExpand = new ImageIcon(Highlander.class.getResource("resources/tree_expand.png"));
  
  //Database icons
  public final static ImageIcon iDb = new ImageIcon(Highlander.class.getResource("resources/db.png"));
  public final static ImageIcon iDbSave = new ImageIcon(Highlander.class.getResource("resources/save.png"));
  public final static ImageIcon iDbLoad = new ImageIcon(Highlander.class.getResource("resources/load.png"));
  public final static ImageIcon iDbAdd = new ImageIcon(Highlander.class.getResource("resources/db_add.png"));
  public final static ImageIcon iDbRemove = new ImageIcon(Highlander.class.getResource("resources/db_remove.png"));
  public final static ImageIcon iDbStatus = new ImageIcon(Highlander.class.getResource("resources/db_status.png"));
  public final static ImageIcon iDbError = new ImageIcon(Highlander.class.getResource("resources/db_error.png"));

  //Lock and permissions icons
  public final static ImageIcon iLock = new ImageIcon(Highlander.class.getResource("resources/lock.png"));
  public final static ImageIcon iUnlock = new ImageIcon(Highlander.class.getResource("resources/unlock.png"));
  public final static ImageIcon iPermissionRefused = new ImageIcon(Highlander.class.getResource("resources/lock_cross.png"));

  //Color status icons
  public final static ImageIcon iShinyBallOrange = new ImageIcon(Highlander.class.getResource("resources/shiny_ball_orange.png"));
  public final static ImageIcon iShinyBallRed = new ImageIcon(Highlander.class.getResource("resources/shiny_ball_red.png"));
  public final static ImageIcon iShinyBallGreen = new ImageIcon(Highlander.class.getResource("resources/shiny_ball_green.png"));
  public final static ImageIcon iShinyBallPink = new ImageIcon(Highlander.class.getResource("resources/shiny_ball_pink.png"));
  
  //Variant lists icons
  public final static ImageIcon iVariantList = new ImageIcon(Highlander.class.getResource("resources/variant_list.png"));
  public final static ImageIcon iVariantListSave = new ImageIcon(Highlander.class.getResource("resources/variant_list_save.png"));
  public final static ImageIcon iVariantListLoad = new ImageIcon(Highlander.class.getResource("resources/variant_list_load.png"));
  
  //Database toolbar
  public final static ImageIcon iColumnSelection = new ImageIcon(Highlander.class.getResource("resources/column_selection.png")); 
  public final static ImageIcon iColumnSelectionNew = new ImageIcon(Highlander.class.getResource("resources/column_selection_new.png")); 

  //Filtering toolbar
  public final static ImageIcon iFilter = new ImageIcon(Highlander.class.getResource("resources/filter.png")); 
  public final static ImageIcon iFilterEdit = new ImageIcon(Highlander.class.getResource("resources/filter_edit.png")); 
  public final static ImageIcon iFilterTree = new ImageIcon(Highlander.class.getResource("resources/tree_view.png")); 
  public final static ImageIcon iFilterAnd = new ImageIcon(Highlander.class.getResource("resources/symbol_and.png")); 
  public final static ImageIcon iFilterOr = new ImageIcon(Highlander.class.getResource("resources/symbol_or.png")); 
  public final static ImageIcon iFilterAddAnd = new ImageIcon(Highlander.class.getResource("resources/filter_add_and.png")); 
  public final static ImageIcon iFilterAddOr = new ImageIcon(Highlander.class.getResource("resources/filter_add_or.png")); 
  public final static ImageIcon iFilterCustom = new ImageIcon(Highlander.class.getResource("resources/filter_custom.png")); 
  public final static ImageIcon iFilterAddCustom = new ImageIcon(Highlander.class.getResource("resources/filter_add_custom.png")); 
  public final static ImageIcon iFilterAddCustomAnd = new ImageIcon(Highlander.class.getResource("resources/filter_add_custom_and.png")); 
  public final static ImageIcon iFilterAddCustomOr = new ImageIcon(Highlander.class.getResource("resources/filter_add_custom_or.png")); 
  public final static ImageIcon iFilterMagic = new ImageIcon(Highlander.class.getResource("resources/filter_magic.png")); 
  public final static ImageIcon iFilterAddMagic = new ImageIcon(Highlander.class.getResource("resources/filter_add_magic.png")); 
  public final static ImageIcon iFilterAddMagicAnd = new ImageIcon(Highlander.class.getResource("resources/filter_add_magic_and.png")); 
  public final static ImageIcon iFilterAddMagicOr = new ImageIcon(Highlander.class.getResource("resources/filter_add_magic_or.png")); 
  public final static ImageIcon iFilterLoadAnd = new ImageIcon(Highlander.class.getResource("resources/load_and.png"));
  public final static ImageIcon iFilterLoadOr = new ImageIcon(Highlander.class.getResource("resources/load_or.png"));

  public final static ImageIcon iCount = new ImageIcon(Highlander.class.getResource("resources/counter.png"));
  public final static ImageIcon iButtonAutoApply = new ImageIcon(Highlander.class.getResource("resources/auto_apply.png"));
  public final static ImageIcon iButtonAutoApplyGrey = new ImageIcon(Highlander.class.getResource("resources/auto_apply_grey.png"));
  
  //Navigation toolbar
  public final static ImageIcon iNavigation = new ImageIcon(Highlander.class.getResource("resources/navigation.png")); 
  public final static ImageIcon iNavigationGlow = new ImageIcon(Highlander.class.getResource("resources/navigation_glow.png")); 
  public final static ImageIcon iSelectionRow = new ImageIcon(Highlander.class.getResource("resources/selection_row.png"));
  public final static ImageIcon iSelectionCell = new ImageIcon(Highlander.class.getResource("resources/selection_cell.png"));
  public final static ImageIcon iColumnMask = new ImageIcon(Highlander.class.getResource("resources/column_mask.png")); 
  public final static ImageIcon iColumnMaskNew = new ImageIcon(Highlander.class.getResource("resources/column_mask_new.png")); 

  //Sorting toolbar
  public final static ImageIcon iSort = new ImageIcon(Highlander.class.getResource("resources/sort.png")); 
  public final static ImageIcon iSortAsc = new ImageIcon(Highlander.class.getResource("resources/sort_asc.png"));
  public final static ImageIcon iSortDesc = new ImageIcon(Highlander.class.getResource("resources/sort_desc.png"));
  public final static ImageIcon iSortAdd = new ImageIcon(Highlander.class.getResource("resources/sort_add.png"));

  //Highlighting toolbar
  public final static ImageIcon iHighlighting = new ImageIcon(Highlander.class.getResource("resources/highlighting.png"));
  public final static ImageIcon iHeatMap = new ImageIcon(Highlander.class.getResource("resources/gradient_rgb_rg.png"));
  public final static ImageIcon iHighlightingAdd = new ImageIcon(Highlander.class.getResource("resources/highlighting_add.png"));
  public final static ImageIcon iHeatMapAdd = new ImageIcon(Highlander.class.getResource("resources/gradient_add.png"));
  public final static ImageIcon iHeatMapRgbRG = new ImageIcon(Highlander.class.getResource("resources/gradient_rgb_rg.png"));
  public final static ImageIcon iHeatMapRgbGR = new ImageIcon(Highlander.class.getResource("resources/gradient_rgb_gr.png"));
  public final static ImageIcon iHeatMapHsvBR = new ImageIcon(Highlander.class.getResource("resources/gradient_hsv_br.png"));
  public final static ImageIcon iHeatMapHsvRB = new ImageIcon(Highlander.class.getResource("resources/gradient_hsv_rb.png"));
  
  //Search toolbar
  public final static ImageIcon iSearch = new ImageIcon(Highlander.class.getResource("resources/search.png")); 
  public final static ImageIcon iSearchGlow = new ImageIcon(Highlander.class.getResource("resources/search_glow.png")); 
  public final static ImageIcon iPressEnter = new ImageIcon(Highlander.class.getResource("resources/key_enter.png"));
  public final static ImageIcon iPressKey = new ImageIcon(Highlander.class.getResource("resources/key_uiojkl.png"));
  public final static ImageIcon iRegExp = new ImageIcon(Highlander.class.getResource("resources/regexp.png"));

  //User profile toolbar
  public final static ImageIcon iUser = new ImageIcon(Highlander.class.getResource("resources/user.png"));
  public final static ImageIcon iUserAdd = new ImageIcon(Highlander.class.getResource("resources/user_add.png"));
  public final static ImageIcon iUserDelete = new ImageIcon(Highlander.class.getResource("resources/user_delete.png"));
  public final static ImageIcon iUserEdit = new ImageIcon(Highlander.class.getResource("resources/user_edit.png"));
  public final static ImageIcon iUserLock = new ImageIcon(Highlander.class.getResource("resources/user_lock.png"));
  public final static ImageIcon iUserPromote = new ImageIcon(Highlander.class.getResource("resources/user_promote.png"));
  public final static ImageIcon iUsers = new ImageIcon(Highlander.class.getResource("resources/users.png"));
  public final static ImageIcon iUsersLock = new ImageIcon(Highlander.class.getResource("resources/users_lock.png")); 

  public final static ImageIcon iUserTree = new ImageIcon(Highlander.class.getResource("resources/user_tree.png")); 
  public final static ImageIcon iFolder = new ImageIcon(Highlander.class.getResource("resources/folder-violet.png"));
  public final static ImageIcon iFolderNew = new ImageIcon(Highlander.class.getResource("resources/folder_new.png")); 
  public final static ImageIcon iField = new ImageIcon(Highlander.class.getResource("resources/selection_column.png"));
  public final static ImageIcon iComments = new ImageIcon(Highlander.class.getResource("resources/comments.png"));

  public final static ImageIcon iList = new ImageIcon(Highlander.class.getResource("resources/list symbol.png"));
  public final static ImageIcon iInterval = new ImageIcon(Highlander.class.getResource("resources/interval.png"));
  public final static ImageIcon iTemplate = new ImageIcon(Highlander.class.getResource("resources/template_filter.png"));
  public final static ImageIcon iUserList = new ImageIcon(Highlander.class.getResource("resources/user_list.png"));
  public final static ImageIcon iUserListEdit = new ImageIcon(Highlander.class.getResource("resources/user_list_edit.png"));
  public final static ImageIcon iUserListEditField = new ImageIcon(Highlander.class.getResource("resources/user_list_edit_fromdb.png"));
  public final static ImageIcon iUserListNew = new ImageIcon(Highlander.class.getResource("resources/user_list_new.png"));
  public final static ImageIcon iUserListNewField = new ImageIcon(Highlander.class.getResource("resources/user_list_new_fromdb.png"));
  public final static ImageIcon iUserListValidate = new ImageIcon(Highlander.class.getResource("resources/user_list_validate.png"));
  public final static ImageIcon iUserListDelete = new ImageIcon(Highlander.class.getResource("resources/user_list_delete.png"));
  public final static ImageIcon iUserListShare = new ImageIcon(Highlander.class.getResource("resources/user_list_share.png"));
  public final static ImageIcon iUserFilter = new ImageIcon(Highlander.class.getResource("resources/user_filter.png"));
  public final static ImageIcon iUserFilterDelete = new ImageIcon(Highlander.class.getResource("resources/user_filter_delete.png"));
  public final static ImageIcon iUserFilterShare = new ImageIcon(Highlander.class.getResource("resources/user_filter_share.png"));
  public final static ImageIcon iUserSorting = new ImageIcon(Highlander.class.getResource("resources/user_sorting.png"));
  public final static ImageIcon iUserSortingDelete = new ImageIcon(Highlander.class.getResource("resources/user_sorting_delete.png"));
  public final static ImageIcon iUserSortingShare = new ImageIcon(Highlander.class.getResource("resources/user_sorting_share.png"));
  public final static ImageIcon iUserHighlightingDelete = new ImageIcon(Highlander.class.getResource("resources/user_highlighting_delete.png"));
  public final static ImageIcon iUserHighlightingShare = new ImageIcon(Highlander.class.getResource("resources/user_highlighting_share.png"));
  public final static ImageIcon iUserColumnSelectionDelete = new ImageIcon(Highlander.class.getResource("resources/user_column_selection_delete.png"));
  public final static ImageIcon iUserColumnSelectionEdit = new ImageIcon(Highlander.class.getResource("resources/user_column_selection_edit.png"));
  public final static ImageIcon iUserColumnSelectionShare = new ImageIcon(Highlander.class.getResource("resources/user_column_selection_share.png"));
  public final static ImageIcon iUserColumnMaskDelete = new ImageIcon(Highlander.class.getResource("resources/user_column_mask_delete.png"));
  public final static ImageIcon iUserColumnMaskEdit = new ImageIcon(Highlander.class.getResource("resources/user_column_mask_edit.png"));
  public final static ImageIcon iUserColumnMaskShare = new ImageIcon(Highlander.class.getResource("resources/user_column_mask_share.png"));
  public final static ImageIcon iUserIntervals = new ImageIcon(Highlander.class.getResource("resources/user_intervals.png"));
  public final static ImageIcon iUserIntervalsNew = new ImageIcon(Highlander.class.getResource("resources/user_intervals_new.png"));
  public final static ImageIcon iUserIntervalsEdit = new ImageIcon(Highlander.class.getResource("resources/user_intervals_edit.png"));
  public final static ImageIcon iUserIntervalsDelete = new ImageIcon(Highlander.class.getResource("resources/user_intervals_delete.png"));
  public final static ImageIcon iUserIntervalsShare = new ImageIcon(Highlander.class.getResource("resources/user_intervals_share.png"));
  public final static ImageIcon iUserTemplate = new ImageIcon(Highlander.class.getResource("resources/user_template.png"));
  public final static ImageIcon iUserTemplateNew = new ImageIcon(Highlander.class.getResource("resources/user_template_new.png"));
  public final static ImageIcon iUserTemplateEdit = new ImageIcon(Highlander.class.getResource("resources/user_template_edit.png"));
  public final static ImageIcon iUserTemplateDelete = new ImageIcon(Highlander.class.getResource("resources/user_template_delete.png"));
  public final static ImageIcon iUserTemplateShare = new ImageIcon(Highlander.class.getResource("resources/user_template_share.png"));
  public final static ImageIcon iUserVariantListDelete = new ImageIcon(Highlander.class.getResource("resources/user_variant_list_delete.png"));
  public final static ImageIcon iUserVariantListShare = new ImageIcon(Highlander.class.getResource("resources/user_variant_list_share.png"));
  public final static ImageIcon iUserFolderShare = new ImageIcon(Highlander.class.getResource("resources/user_folder_share.png"));
  public final static ImageIcon iUserCheckShare = new ImageIcon(Highlander.class.getResource("resources/user_check_share.png"));
  
  //Tools toolbar
  public final static ImageIcon iTools = new ImageIcon(Highlander.class.getResource("resources/tools.png")); 
  public final static ImageIcon iIGV = new ImageIcon(Highlander.class.getResource("resources/igv.png"));
  public final static ImageIcon iIGVpos = new ImageIcon(Highlander.class.getResource("resources/igv_position.png"));
  public final static ImageIcon iExcel = new ImageIcon(Highlander.class.getResource("resources/excel.png"));
  public final static ImageIcon iExcelTN = new ImageIcon(Highlander.class.getResource("resources/excel_TN.png"));
  public final static ImageIcon iTSV = new ImageIcon(Highlander.class.getResource("resources/tsv_export.png"));
  public final static ImageIcon iVCF = new ImageIcon(Highlander.class.getResource("resources/vcf_export.png"));
  public final static ImageIcon iBurdenTest = new ImageIcon(Highlander.class.getResource("resources/burden_test.png"));
  public final static ImageIcon iBamViewer = new ImageIcon(Highlander.class.getResource("resources/bam_viewer.png"));
  public final static ImageIcon iBamChecker = new ImageIcon(Highlander.class.getResource("resources/bam_checker.png"));
  public final static ImageIcon iStatAssociator = new ImageIcon(Highlander.class.getResource("resources/stat_associator.png"));
  public final static ImageIcon iPedigreeChecker = new ImageIcon(Highlander.class.getResource("resources/pedigree_checker.png"));
  public final static ImageIcon iPedigreeCheckerCommon = new ImageIcon(Highlander.class.getResource("resources/pedcheck_common.png"));
  public final static ImageIcon iPedigreeCheckerAdjusted = new ImageIcon(Highlander.class.getResource("resources/pedcheck_adjusted.png"));
  public final static ImageIcon iPedigreeMale = new ImageIcon(Highlander.class.getResource("resources/gender_male.png"));
  public final static ImageIcon iPedigreeFemale = new ImageIcon(Highlander.class.getResource("resources/gender_female.png"));
  public final static ImageIcon iCoverage = new ImageIcon(Highlander.class.getResource("resources/coverage.png"));
  public final static ImageIcon iDownload = new ImageIcon(Highlander.class.getResource("resources/folder-download.png"));
  public final static ImageIcon iRunStatisticsCharts = new ImageIcon(Highlander.class.getResource("resources/run_statistics_charts.png"));
  public final static ImageIcon iRunStatisticsDetails = new ImageIcon(Highlander.class.getResource("resources/run_statistics_details.png"));
  public final static ImageIcon iFastQC = new ImageIcon(Highlander.class.getResource("resources/fastqc.png"));
  public final static ImageIcon iChartDouble = new ImageIcon(Highlander.class.getResource("resources/chart_double.png"));
  public final static ImageIcon iChartBar = new ImageIcon(Highlander.class.getResource("resources/chart_bar.png"));
  public final static ImageIcon iChartPie = new ImageIcon(Highlander.class.getResource("resources/chart_pie.png"));
  public final static ImageIcon iRunSummary = new ImageIcon(Highlander.class.getResource("resources/run_summary.png"));
  public final static ImageIcon iRunReport = new ImageIcon(Highlander.class.getResource("resources/run_report.png"));
  public final static ImageIcon iCTDNA = new ImageIcon(Highlander.class.getResource("resources/ctDNA.png"));
  public final static ImageIcon iHGMD = new ImageIcon(Highlander.class.getResource("resources/HGMD.png"));
  public final static ImageIcon iAlignmentPinned = new ImageIcon(Highlander.class.getResource("resources/alignment_pinned.png"));

  //Burden Test
  public final static ImageIcon iZoomIn = new ImageIcon(Highlander.class.getResource("resources/zoom_in.png"));
  public final static ImageIcon iZoomOut = new ImageIcon(Highlander.class.getResource("resources/zoom_out.png"));
  public final static ImageIcon iZoomOriginal = new ImageIcon(Highlander.class.getResource("resources/zoom_original.png"));
  public final static ImageIcon iZoomBestFit = new ImageIcon(Highlander.class.getResource("resources/zoom_fit_best.png"));
  public final static ImageIcon iChiSquare = new ImageIcon(Highlander.class.getResource("resources/chi_square.png"));

  //Alignment detail box
  public final static ImageIcon iAlignmentCenterMutation = new ImageIcon(Highlander.class.getResource("resources/center_mutation.png"));
  public final static ImageIcon iAlignmentSoftclipOn = new ImageIcon(Highlander.class.getResource("resources/alignment_softclip_on.png"));
  public final static ImageIcon iAlignmentSoftclipOff = new ImageIcon(Highlander.class.getResource("resources/alignment_softclip_off.png"));
  public final static ImageIcon iAlignmentSquishedOn = new ImageIcon(Highlander.class.getResource("resources/alignment_squished_on.png"));
  public final static ImageIcon iAlignmentSquishedOff = new ImageIcon(Highlander.class.getResource("resources/alignment_squished_off.png"));
  public final static ImageIcon iAlignmentFrameShiftOn = new ImageIcon(Highlander.class.getResource("resources/alignment_frameshift_on.png"));
  public final static ImageIcon iAlignmentFrameShiftOff = new ImageIcon(Highlander.class.getResource("resources/alignment_frameshift_off.png"));
  
  //Help toolbar
  public final static ImageIcon iHelp = new ImageIcon(Highlander.class.getResource("resources/help.png")); 
  public final static ImageIcon iMemory = new ImageIcon(Highlander.class.getResource("resources/memory.png"));
  public final static ImageIcon iLucky = new ImageIcon(Highlander.class.getResource("resources/lucky.png"));
  public final static ImageIcon iBadLuck = new ImageIcon(Highlander.class.getResource("resources/bad_luck.jpg"));
  public final static ImageIcon iAbout = new ImageIcon(Highlander.class.getResource("resources/about.png"));
  
  //Miscellaneous
  public final static ImageIcon iUpdater = new ImageIcon(Highlander.class.getResource("resources/updater_32.png"));
  public final static ImageIcon iLastDbAdditions = new ImageIcon(Highlander.class.getResource("resources/last_db_additions.png"));
  public final static ImageIcon iPatients = new ImageIcon(Highlander.class.getResource("resources/patients.png"));
  public final static ImageIcon iDbSearch = new ImageIcon(Highlander.class.getResource("resources/db_load.png"));
  public final static ImageIcon iReference = new ImageIcon(Highlander.class.getResource("resources/reference.png"));
  
  //Logos
  public final static ImageIcon iLogoHighlander = new ImageIcon(Highlander.class.getResource("resources/logo_highlander.png"));
  public final static ImageIcon iLogoGEHU = new ImageIcon(Highlander.class.getResource("resources/logo_gehu.png"));
  public final static ImageIcon iLogoDeDuveUCLouvain = new ImageIcon(Highlander.class.getResource("resources/logo_dduv_ucl.png"));
  public final static ImageIcon iLogoDeDuveHorizontal = new ImageIcon(Highlander.class.getResource("resources/logo_deduve_hori.png"));
  public final static ImageIcon iLogoDeDuveVertival = new ImageIcon(Highlander.class.getResource("resources/logo_deduve_vert.png"));
  public final static ImageIcon iLogoFCE = new ImageIcon(Highlander.class.getResource("resources/logo_fondation_contre_le_cancer.png"));
  public final static ImageIcon iLogoInnoviris = new ImageIcon(Highlander.class.getResource("resources/logo_innoviris.png"));
  public final static ImageIcon iLogoUCLouvainHorizontal = new ImageIcon(Highlander.class.getResource("resources/logo_uclouvain_hori.png"));
  public final static ImageIcon iLogoUCLouvainVertical = new ImageIcon(Highlander.class.getResource("resources/logo_uclouvain_vert.png"));
  public final static ImageIcon iLogoWelbio = new ImageIcon(Highlander.class.getResource("resources/logo_welbio.png"));

  //External web resources
  public final static ImageIcon iExtBeacon = new ImageIcon(Highlander.class.getResource("resources/ext_beacon.png"));
  public final static ImageIcon iExtCliniphenome = new ImageIcon(Highlander.class.getResource("resources/ext_cliniphenome.png"));
  public final static ImageIcon iExtCosmic = new ImageIcon(Highlander.class.getResource("resources/ext_cosmic.png"));
  public final static ImageIcon iExtDbnsp = new ImageIcon(Highlander.class.getResource("resources/ext_dbsnp.png"));
  public final static ImageIcon iExtDecipher = new ImageIcon(Highlander.class.getResource("resources/ext_decipher.png"));
  public final static ImageIcon iExtDida = new ImageIcon(Highlander.class.getResource("resources/ext_dida.png"));
  public final static ImageIcon iExtEnsembl = new ImageIcon(Highlander.class.getResource("resources/ext_ensembl.png"));
  public final static ImageIcon iExtEntrez = new ImageIcon(Highlander.class.getResource("resources/ext_entrez.png"));
  public final static ImageIcon iExtExac = new ImageIcon(Highlander.class.getResource("resources/ext_exac.png"));
  public final static ImageIcon iExtGnomad = new ImageIcon(Highlander.class.getResource("resources/ext_gnomad.png"));
  public final static ImageIcon iExtHgnc = new ImageIcon(Highlander.class.getResource("resources/ext_hgnc.png"));
  public final static ImageIcon iExtLovd = new ImageIcon(Highlander.class.getResource("resources/ext_lovd.png"));
  public final static ImageIcon iExtMarrvel = new ImageIcon(Highlander.class.getResource("resources/ext_marrvel.png"));
  public final static ImageIcon iExtMutaframe = new ImageIcon(Highlander.class.getResource("resources/ext_mutaframe.png"));
  public final static ImageIcon iExtMutalyzer = new ImageIcon(Highlander.class.getResource("resources/ext_mutalyzer.png"));
  public final static ImageIcon iExtMutationTaster = new ImageIcon(Highlander.class.getResource("resources/ext_mutation_taster.png"));
  public final static ImageIcon iExtNcbi = new ImageIcon(Highlander.class.getResource("resources/ext_ncbi.png"));
  public final static ImageIcon iExtNhgriBic = new ImageIcon(Highlander.class.getResource("resources/ext_NHGRI_BIC.png"));
  public final static ImageIcon iExtOmim = new ImageIcon(Highlander.class.getResource("resources/ext_omim.png"));
  public final static ImageIcon iExtPubmed = new ImageIcon(Highlander.class.getResource("resources/ext_pubmed.png"));
  public final static ImageIcon iExtUcsc = new ImageIcon(Highlander.class.getResource("resources/ext_ucsc.png"));
  public final static ImageIcon iExtClinVar = new ImageIcon(Highlander.class.getResource("resources/ext_clinvar.png"));
  public final static ImageIcon iExtClinVarMiner = new ImageIcon(Highlander.class.getResource("resources/ext_clinvarminer.png"));
  public final static ImageIcon iExtFranklin = new ImageIcon(Highlander.class.getResource("resources/ext_franklin.png"));
  public final static ImageIcon iExtGtex = new ImageIcon(Highlander.class.getResource("resources/ext_gtex.png"));
  public final static ImageIcon iExtUniprot = new ImageIcon(Highlander.class.getResource("resources/ext_uniprot.png"));
  public final static ImageIcon iExtVarsome = new ImageIcon(Highlander.class.getResource("resources/ext_varsome.png"));

  public static ImageIcon getScaledIcon(ImageIcon icon, int size){
  	return new ImageIcon(icon.getImage().getScaledInstance(size, size,  java.awt.Image.SCALE_SMOOTH));
  }
   	
  public static ImageIcon getHeightScaledIcon(ImageIcon icon, int height){
  	return new ImageIcon(icon.getImage().getScaledInstance(-1, height,  java.awt.Image.SCALE_SMOOTH));
  }
  
  public static ImageIcon getWidthScaledIcon(ImageIcon icon, int width){
  	return new ImageIcon(icon.getImage().getScaledInstance(width, -1, java.awt.Image.SCALE_SMOOTH));
  }
  
  public static ImageIcon getColoredSquare(int size, Color color){
  	BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);  	
  	Graphics g = image.getGraphics();
  	g.setColor(new Color(0, 0, 0, 0));
  	g.fillRect(0, 0, size, size);
  	g.setColor(color);
		g.fillRect(1, 1, size, size);
  	g.setColor(Color.BLACK);
		g.drawRect(1, 1, size, size);
		return new ImageIcon(image);
  }
}
