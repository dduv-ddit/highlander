/*****************************************************************************************
*
* Highlander - Copyright (C) <2012-2020> <Universit� catholique de Louvain (UCLouvain)>
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

package be.uclouvain.ngs.highlander.administration.dbpatcher;


/**
* @author Raphael Helaers
*/

public class Version_Development extends Version {

	public Version_Development() {
		super("2000");
	}

	@Override
	protected void makeUpdate() throws Exception {
		
		/* Pouvoir "fixer"/"freeze" un certain nombre de colonnes sur la gauche de la table principal, comme on peut le faire dans Excel
		 * 
		 * De base la solution qui retire X colonnes de la table vers une seconde table "fixed" plac�e dans le rowheader du scrollpane fonctionne.
		 * Ca se complique quand il faut ajuster toutes les autres propri�t�s : 
		 * - sorter -> il semblerait que 2 tables puissent partager le m�me sorter
		 * - Highlighting -> marche pas de base, � voir avec partage
		 * - Masques -> pas test�
		 * En gros je vais devoir remplacer "table" dans VariantTable par 2 tables, et tracker tous les appels pour tout faire en double
		 * Je ne sais pas trop comment �a r�agit aux modifications (quel est l'id des colonnes apr�s suppression des fixed ? sont-elles toujours dans le modele ?)
		 * En tout cas choisir des colonnes � trier n'affichent plus les colonnes freezed ...
		 *
		 * Peut-�tre ne pas supprimer les colonnes de la table principale, mais simplement les masquer et les dupliquer dans la table fixed.
		 * Ca pourrait permettre de continuer � travailler uniquement avec la table principale en dehors de VariantTable, et r�soudrait les probl�mes de colonne manquante. 
		 * 
		 * --> C'est pas gagn�, �a va demander beaucoup de boulot.
		 *  
		 */

		//Hikari connection leak ? Probleme il se passe rien et les gens cliquent plein de fois. Timer ?
		//Ca serait d� soit � la machine qui passe en veille, soit � un manque de ressource (memoire ou CPU)
		
		//search garder en m�moire l'�tat du enter
		//Pedigree Checker cadre
		//#SD dans les charts
		
		//Aligmment
		//Ajout d'une version "split" du bam viewer entre 2 positions (donc montrer � gauche de pos 1 et � droite de pos 2 mais rien entre les 2
		//--> Les mismatch et soft clipped peuvent �tre montr�s quand ils d�passent sur l'autre partie
		//Ajout du coverage.
		//--> Garder le compte des nucl�otiques pour chaque position, comme �a je peux aussi montrer les d�tails dans le tooltip.
		//--> Pas forc�ment simple quand je pin plusieurs alignements ... superposer le coverage avec des couleurs diff�rentes et transparentes ?
		//Liste des features pr�sentes, et pouvoir se d�placer de l'une � l'autre (dans le viewer s�par�, pas dans la detail box)
		
		//Groupes d'utilisateurs

		//Dossier commun

		//CNV: outil alternatif g�n�ral pour la box alignment ou detail box sp�cifique par caller (exomeDepth, QDNAseq, ...) ?
		//Make the bam visible on BOTH CNV breakpoints (slight difference with the SNV); so we could see the split reads, the read rainbow effect at the breakpoints, etc
		//CNV: que la colonne num_genes refl�te le nombre de g�nes touch�s par le CNV
		//Voir les suggestions de Guillaume pour les filtres magiques et les CNV
		//detail box qui reprendrait les infos du CNV complet, difficiles � rassembler via la table en cas de filtre: g�nes touch�s, total des SNV het/hom (voir si y a autre chose), cnv_exons total
		//+local frequency du complet (=loca_af), du gene uniquement et de l'exon uniquement. + pouvoir choisir un % d'overlap pour calculer ces fr�quences.
		
	}

}
