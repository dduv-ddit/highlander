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
		 * De base la solution qui retire X colonnes de la table vers une seconde table "fixed" placée dans le rowheader du scrollpane fonctionne.
		 * Ca se complique quand il faut ajuster toutes les autres propriétés : 
		 * - sorter -> il semblerait que 2 tables puissent partager le même sorter
		 * - Highlighting -> marche pas de base, à voir avec partage
		 * - Masques -> pas testé
		 * En gros je vais devoir remplacer "table" dans VariantTable par 2 tables, et tracker tous les appels pour tout faire en double
		 * Je ne sais pas trop comment ça réagit aux modifications (quel est l'id des colonnes après suppression des fixed ? sont-elles toujours dans le modele ?)
		 * En tout cas choisir des colonnes à trier n'affichent plus les colonnes freezed ...
		 *
		 * Peut-être ne pas supprimer les colonnes de la table principale, mais simplement les masquer et les dupliquer dans la table fixed.
		 * Ca pourrait permettre de continuer à travailler uniquement avec la table principale en dehors de VariantTable, et résoudrait les problèmes de colonne manquante. 
		 * 
		 * --> C'est pas gagné, ça va demander beaucoup de boulot.
		 *  
		 */

		//Hikari connection leak ? Probleme il se passe rien et les gens cliquent plein de fois. Timer ?
		//Ca serait dû soit à la machine qui passe en veille, soit à un manque de ressource (memoire ou CPU)
		
		//search garder en mémoire l'état du enter
		//Pedigree Checker cadre
		//#SD dans les charts
		
		//Aligmment
		//Ajout d'une version "split" du bam viewer entre 2 positions (donc montrer à gauche de pos 1 et à droite de pos 2 mais rien entre les 2
		//--> Les mismatch et soft clipped peuvent être montrés quand ils dépassent sur l'autre partie
		//Ajout du coverage.
		//--> Garder le compte des nucléotiques pour chaque position, comme ça je peux aussi montrer les détails dans le tooltip.
		//--> Pas forcément simple quand je pin plusieurs alignements ... superposer le coverage avec des couleurs différentes et transparentes ?
		//Liste des features présentes, et pouvoir se déplacer de l'une à l'autre (dans le viewer séparé, pas dans la detail box)
		
		//Groupes d'utilisateurs

		//Dossier commun

		//CNV: outil alternatif général pour la box alignment ou detail box spécifique par caller (exomeDepth, QDNAseq, ...) ?
		//Make the bam visible on BOTH CNV breakpoints (slight difference with the SNV); so we could see the split reads, the read rainbow effect at the breakpoints, etc
		//CNV: que la colonne num_genes reflète le nombre de gènes touchés par le CNV
		//Voir les suggestions de Guillaume pour les filtres magiques et les CNV
		//detail box qui reprendrait les infos du CNV complet, difficiles à rassembler via la table en cas de filtre: gènes touchés, total des SNV het/hom (voir si y a autre chose), cnv_exons total
		//+local frequency du complet (=loca_af), du gene uniquement et de l'exon uniquement. + pouvoir choisir un % d'overlap pour calculer ces fréquences.
		
	}

}
