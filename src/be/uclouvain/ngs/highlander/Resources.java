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
import java.awt.Image;
import java.awt.image.BaseMultiResolutionImage;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import javax.imageio.ImageIO;
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
	
	
	/*
	 * Expression régulière pour remplacer l'ancienne version
	 * 
	 * (Resources\.getScaledIcon\(Resources.i)([A-Za-z0-9]+)(, *)([0-9]+)(\))
	 * ->
	 * Img.\2.getScaledIcon(\4)
	 * 
	 */
	public enum Img {
	  Highlander("highlander.png"),
	  AdminstrationTools("administration_tools.png"),
	  IonImporter("analysis_iontorrent.png"),
	  ProjectManager("project_manager.png"),
	  DbPatcher("db_patcher.png"),

	  //Waiting animation
	  Wait0 ("waiting_0.png"),
	  Wait1 ("waiting_1.png"),
	  Wait2 ("waiting_2.png"),
	  Wait3 ("waiting_3.png"),
	  Wait4 ("waiting_4.png"),
	  Wait5 ("waiting_5.png"),
	  Wait6 ("waiting_6.png"),
	  Wait7 ("waiting_7.png"),
	  Wait8 ("waiting_8.png"),
	  Loading ("loading.png"),
	  
	  //General buttons
	  ButtonApply ("checked.png"),
	  Cross ("cross.png"),
	  Attention ("attention.png"),
	  Question ("question.png"),
	  Roman1 ("roman_1.png"),
	  Roman2 ("roman_2.png"),
	  Roman3 ("roman_3.png"),
	  Roman4 ("roman_4.png"),
	  Roman5 ("roman_5.png"),
	  Exit ("exit.png"),
	  Save ("save.png"),
	  Load ("load.png"),
	  ExportFile ("export_file.png"),
	  ImportFile ("folder-green.png"),
	  ExportJpeg ("export_jpeg.png"),
	  SortAZ ("sort_AZ.png"),
	  EditWrench ("edit_wrench.png"),
	  EditPen ("edit_pen.png"),
	  Printer ("printer.png"),
	  Copy ("edit-copy.png"),
	  Pin ("pin.png"),
	  Unpin ("unpin.png"),
	  Reset ("reset.png"),
	  Run ("run.png"),

	  //Main add/remove button
	  AddMain ("pm_3d_plus.png"),
	  RemoveMain ("pm_3d_minus.png"),
	  //Secondary add/remove button (when the main one is already in use in the frame, like magic filters)
	  AddSecondary ("pm_faint_plus.png"),
	  RemoveSecondary ("pm_faint_minus.png"),
	  //Expand/collapse button (e.g. details boxes)
	  Expand ("pm_2d_plus.png"),
	  Collapse ("pm_2d_minus.png"),
	  //Unused plus/minus
	  IsometricPlus ("pm_isometric_plus.png"),
	  IsometricMinus ("pm_isometric_minus.png"),

	  //Arrows icons
	  ArrowRight ("arrow-right.png"),
	  ArrowLeft ("arrow-left.png"),
	  ArrowDoubleRight ("arrow-right-double.png"),
	  ArrowDoubleLeft ("arrow-left-double.png"),
	  ArrowDoubleUp ("arrow-up-double.png"),
	  ArrowDoubleDown ("arrow-down-double.png"),

	  //Tree icons
	  TreeExpand ("tree_expand.png"),
	  
	  //Database icons
	  Db ("db.png"),
	  DbSave ("save.png"),
	  DbLoad ("load.png"),
	  DbAdd ("db_add.png"),
	  DbRemove ("db_remove.png"),
	  DbStatus ("db_status.png"),
	  DbError ("db_error.png"),

	  //Lock and permissions icons
	  Lock ("lock.png"),
	  Unlock ("unlock.png"),
	  PermissionRefused ("lock_cross.png"),

	  //Color status icons
	  ShinyBallOrange ("shiny_ball_orange.png"),
	  ShinyBallRed ("shiny_ball_red.png"),
	  ShinyBallGreen ("shiny_ball_green.png"),
	  ShinyBallPink ("shiny_ball_pink.png"),
	  
	  //Variant lists icons
	  VariantList ("variant_list.png"),
	  VariantListSave ("variant_list_save.png"),
	  VariantListLoad ("variant_list_load.png"),
	  
	  //Database toolbar
	  ColumnSelection ("column_selection.png"), 
	  ColumnSelectionNew ("column_selection_new.png"), 

	  //Filtering toolbar
	  Filter ("filter.png"), 
	  FilterEdit ("filter_edit.png"), 
	  FilterTree ("tree_view.png"), 
	  FilterAnd ("symbol_and.png"), 
	  FilterOr ("symbol_or.png"), 
	  FilterAddAnd ("filter_add_and.png"), 
	  FilterAddOr ("filter_add_or.png"), 
	  FilterCustom ("filter_custom.png"), 
	  FilterAddCustom ("filter_add_custom.png"), 
	  FilterAddCustomAnd ("filter_add_custom_and.png"), 
	  FilterAddCustomOr ("filter_add_custom_or.png"), 
	  FilterMagic ("filter_magic.png"), 
	  FilterAddMagic ("filter_add_magic.png"), 
	  FilterAddMagicAnd ("filter_add_magic_and.png"), 
	  FilterAddMagicOr ("filter_add_magic_or.png"), 
	  FilterLoadAnd ("load_and.png"),
	  FilterLoadOr ("load_or.png"),

	  Count ("counter.png"),
	  ButtonAutoApply ("auto_apply.png"),
	  ButtonAutoApplyGrey ("auto_apply_grey.png"),
	  
	  //Navigation toolbar
	  Navigation ("navigation.png"), 
	  NavigationGlow ("navigation_glow.png"), 
	  SelectionRow ("selection_row.png"),
	  SelectionCell ("selection_cell.png"),
	  ColumnMask ("column_mask.png"), 
	  ColumnMaskNew ("column_mask_new.png"), 

	  //Sorting toolbar
	  Sort ("sort.png"), 
	  SortAsc ("sort_asc.png"),
	  SortDesc ("sort_desc.png"),
	  SortAdd ("sort_add.png"),

	  //Highlighting toolbar
	  Highlighting ("highlighting.png"),
	  HeatMap ("gradient_rgb_rg.png"),
	  HighlightingAdd ("highlighting_add.png"),
	  HeatMapAdd ("gradient_add.png"),
	  HeatMapRgbRG ("gradient_rgb_rg.png"),
	  HeatMapRgbGR ("gradient_rgb_gr.png"),
	  HeatMapHsvBR ("gradient_hsv_br.png"),
	  HeatMapHsvRB ("gradient_hsv_rb.png"),
	  
	  //Search toolbar
	  Search ("search.png"), 
	  SearchGlow ("search_glow.png"), 
	  PressEnter ("key_enter.png"),
	  PressKey ("key_uiojkl.png"),
	  RegExp ("regexp.png"),

	  //User profile toolbar
	  User ("user.png"),
	  UserAdd ("user_add.png"),
	  UserDelete ("user_delete.png"),
	  UserEdit ("user_edit.png"),
	  UserLock ("user_lock.png"),
	  UserPromote ("user_promote.png"),
	  Users ("users.png"),
	  UsersLock ("users_lock.png"), 

	  UserTree ("user_tree.png"), 
	  Folder ("folder-violet.png"),
	  FolderNew ("folder_new.png"), 
	  Field ("selection_column.png"),
	  Comments ("comments.png"),

	  List ("list symbol.png"),
	  Interval ("interval.png"),
	  HPO ("hpo.png"),
	  HPOToGenes ("hpo_to_genes.png"),
	  Template ("template_filter.png"),
	  UserList ("user_list.png"),
	  UserListEdit ("user_list_edit.png"),
	  UserListEditField ("user_list_edit_fromdb.png"),
	  UserListNew ("user_list_new.png"),
	  UserListNewField ("user_list_new_fromdb.png"),
	  UserListValidate ("user_list_validate.png"),
	  UserListDelete ("user_list_delete.png"),
	  UserListShare ("user_list_share.png"),
	  UserFilter ("user_filter.png"),
	  UserFilterDelete ("user_filter_delete.png"),
	  UserFilterShare ("user_filter_share.png"),
	  UserSorting ("user_sorting.png"),
	  UserSortingDelete ("user_sorting_delete.png"),
	  UserSortingShare ("user_sorting_share.png"),
	  UserHighlightingDelete ("user_highlighting_delete.png"),
	  UserHighlightingShare ("user_highlighting_share.png"),
	  UserColumnSelectionDelete ("user_column_selection_delete.png"),
	  UserColumnSelectionEdit ("user_column_selection_edit.png"),
	  UserColumnSelectionShare ("user_column_selection_share.png"),
	  UserColumnMaskDelete ("user_column_mask_delete.png"),
	  UserColumnMaskEdit ("user_column_mask_edit.png"),
	  UserColumnMaskShare ("user_column_mask_share.png"),
	  UserIntervals ("user_intervals.png"),
	  UserIntervalsNew ("user_intervals_new.png"),
	  UserIntervalsEdit ("user_intervals_edit.png"),
	  UserIntervalsDelete ("user_intervals_delete.png"),
	  UserIntervalsShare ("user_intervals_share.png"),
	  UserHPO ("user_hpo.png"),
	  UserHPONew ("user_hpo_new.png"),
	  UserHPOEdit ("user_hpo_edit.png"),
	  UserHPODelete ("user_hpo_delete.png"),
	  UserHPOShare ("user_hpo_share.png"),
	  UserTemplate ("user_template.png"),
	  UserTemplateNew ("user_template_new.png"),
	  UserTemplateEdit ("user_template_edit.png"),
	  UserTemplateDelete ("user_template_delete.png"),
	  UserTemplateShare ("user_template_share.png"),
	  UserVariantListDelete ("user_variant_list_delete.png"),
	  UserVariantListShare ("user_variant_list_share.png"),
	  UserFolderShare ("user_folder_share.png"),
	  UserCheckShare ("user_check_share.png"),
	  
	  //Tools toolbar
	  Tools ("tools.png"), 
	  Pavian ("pavian.png"),
	  IGV ("igv.png"),
	  IGVpos ("igv_position.png"),
	  ExportSequence ("export_mutated_sequence.png"),
	  Excel ("excel.png"),
	  ExcelTN ("excel_TN.png"),
	  TSV ("tsv_export.png"),
	  VCF ("vcf_export.png"),
	  BurdenTest ("burden_test.png"),
	  BamViewer ("bam_viewer.png"),
	  BamChecker ("bam_checker.png"),
	  StatAssociator ("stat_associator.png"),
	  PedigreeChecker ("pedigree_checker.png"),
	  PedigreeCheckerCommon ("pedcheck_common.png"),
	  PedigreeCheckerAdjusted ("pedcheck_adjusted.png"),
	  PedigreeMale ("gender_male.png"),
	  PedigreeFemale ("gender_female.png"),
	  Coverage ("coverage.png"),
	  Download ("folder-download.png"),
	  RunStatisticsCharts ("run_statistics_charts.png"),
	  RunStatisticsDetails ("run_statistics_details.png"),
	  FastQC ("fastqc.png"),
	  ChartDouble ("chart_double.png"),
	  ChartBar ("chart_bar.png"),
	  ChartPie ("chart_pie.png"),
	  RunSummary ("run_summary.png"),
	  RunReport ("run_report.png"),
	  CTDNA ("ctDNA.png"),
	  HGMD ("HGMD.png"),
	  Exomiser ("exomiser.png"),
	  Kraken ("krona.png"),
	  AlignmentPinned ("alignment_pinned.png"),
	  M6A ("M6A.png"),

	  //Burden Test
	  ZoomIn ("zoom_in.png"),
	  ZoomOut ("zoom_out.png"),
	  ZoomOriginal ("zoom_original.png"),
	  ZoomBestFit ("zoom_fit_best.png"),
	  ChiSquare ("chi_square.png"),

	  //Alignment detail box
	  AlignmentCenterMutation ("center_mutation.png"),
	  AlignmentSoftclipOn ("alignment_softclip_on.png"),
	  AlignmentSoftclipOff ("alignment_softclip_off.png"),
	  AlignmentSquishedOn ("alignment_squished_on.png"),
	  AlignmentSquishedOff ("alignment_squished_off.png"),
	  AlignmentFrameShiftOn ("alignment_frameshift_on.png"),
	  AlignmentFrameShiftOff ("alignment_frameshift_off.png"),
	  
	  //Help toolbar
	  Help ("help.png"), 
	  Memory ("memory.png"),
	  Lucky ("lucky.png"),
	  BadLuck ("bad_luck.jpg"),
	  About ("about.png"),
	  
	  //Miscellaneous
	  Updater ("updater_32.png"),
	  LastDbAdditions ("last_db_additions.png"),
	  Patients ("patients.png"),
	  DbSearch ("db_load.png"),
	  Reference ("reference.png"),
	  
	  //Logos
	  LogoHighlander ("logo_highlander.png"),
	  LogoGEHU ("logo_gehu.png"),
	  LogoDeDuveUCLouvain ("logo_dduv_ucl.png"),
	  LogoDeDuveHorizontal ("logo_deduve_hori.png"),
	  LogoDeDuveVertival ("logo_deduve_vert.png"),
	  LogoFCE ("logo_fondation_contre_le_cancer.png"),
	  LogoInnoviris ("logo_innoviris.png"),
	  LogoUCLouvainHorizontal ("logo_uclouvain_hori.png"),
	  LogoUCLouvainVertical ("logo_uclouvain_vert.png"),
	  LogoWelbio ("logo_welbio.png"),

	  //External web resources
	  ExtBeacon ("ext_beacon.png"),
	  ExtCliniphenome ("ext_cliniphenome.png"),
	  ExtCosmic ("ext_cosmic.png"),
	  ExtDbnsp ("ext_dbsnp.png"),
	  ExtDecipher ("ext_decipher.png"),
	  ExtDida ("ext_dida.png"),
	  ExtEnsembl ("ext_ensembl.png"),
	  ExtEntrez ("ext_entrez.png"),
	  ExtExac ("ext_exac.png"),
	  ExtGnomad ("ext_gnomad.png"),
	  ExtHgnc ("ext_hgnc.png"),
	  ExtLovd ("ext_lovd.png"),
	  ExtMarrvel ("ext_marrvel.png"),
	  ExtMutaframe ("ext_mutaframe.png"),
	  ExtMutalyzer ("ext_mutalyzer.png"),
	  ExtMutationTaster ("ext_mutation_taster.png"),
	  ExtNcbi ("ext_ncbi.png"),
	  ExtNhgriBic ("ext_NHGRI_BIC.png"),
	  ExtOmim ("ext_omim.png"),
	  ExtPubmed ("ext_pubmed.png"),
	  ExtUcsc ("ext_ucsc.png"),
	  ExtClinVar ("ext_clinvar.png"),
	  ExtClinVarMiner ("ext_clinvarminer.png"),
	  ExtFranklin ("ext_franklin.png"),
	  ExtGtex ("ext_gtex.png"),
	  ExtUniprot ("ext_uniprot.png"),
	  ExtVarsome ("ext_varsome.png"),
		;
		private final String filename;
		private BufferedImage image;
		
		Img(String filename) {
			this.filename = filename;
		}
		
		public String getResourcePath() {
			return "resources/"+filename;
		}
		
		public String getFullResourcePath() {
			return new ImageIcon(Highlander.class.getResource(getResourcePath())).toString();
		}
		
		public Image getImage() {
			if (image == null) {
				try {
					image = ImageIO.read(Highlander.class.getResource(getResourcePath()));					
				}catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			return image;
		}
		
	  public ImageIcon getNonScalableIcon(){
	  	return new ImageIcon(getImage());
	  }
	  
	  public ImageIcon getScaledIcon(int size){
	  	return Resources.getScaledIcon(getImage(), size);
	  }

	  public ImageIcon getHeightScaledIcon(int height){
	  	return Resources.getHeightScaledIcon(getImage(), height);
	  }
	  
	  public ImageIcon getWidthScaledIcon(int width){
	  	return Resources.getWidthScaledIcon(getImage(), width);
	  }

	}

	//Zoom factors available in Windows 10 for HDPI screens
	public final static double[] zoomFactors = {1.0, 1.25, 1.5, 1.75, 2.0, 2.25, 2.5, 2.75, 3.0};

  public static ImageIcon getScaledIcon(Image image, int width, int height){
  	var myImages = new ArrayList<Image>();
  	for (double zoom : zoomFactors) {
  		myImages.add(image.getScaledInstance((int)(width * zoom), (int)(height * zoom),  java.awt.Image.SCALE_SMOOTH));				
  	}
  	return new ImageIcon(new BaseMultiResolutionImage(myImages.toArray(new Image[0])));
  }

  public static ImageIcon getScaledIcon(Image image, int size){
  	return getScaledIcon(image, size, size);
  }
  
  public static ImageIcon getHeightScaledIcon(Image image, int height){
  	return getScaledIcon(image, -1, height);
  }
  
  public static ImageIcon getWidthScaledIcon(Image image, int width){
  	return getScaledIcon(image, width, -1);
  }
  
  public static ImageIcon getColoredSquare(int size, Color color){
  	var myImages = new ArrayList<Image>();
  	for (double zoom : zoomFactors) {
  		int scale = (int)(size * zoom);
    	BufferedImage image = new BufferedImage(scale, scale, BufferedImage.TYPE_INT_ARGB);  	
    	Graphics g = image.getGraphics();
    	g.setColor(new Color(0, 0, 0, 0));
    	g.fillRect(0, 0, scale, scale);
    	g.setColor(color);
  		g.fillRect(1, 1, scale, scale);
    	g.setColor(Color.BLACK);
  		g.drawRect(1, 1, scale, scale);
  		myImages.add(image);				
  	}
  	return new ImageIcon(new BaseMultiResolutionImage(myImages.toArray(new Image[0])));
  }
  
}
