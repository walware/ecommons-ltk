/*=============================================================================#
 # Copyright (c) 2000-2016 IBM Corporation and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     IBM Corporation - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.internal.buildpaths.ui;

import org.eclipse.osgi.util.NLS;


public class Messages {
	
	
	public static String ListLabel_Element_Unknown_label;
	
	public static String ListLabel_Attribute_Generic_label;
	public static String ListLabel_Attribute_SourceAttachment_label;
	public static String ListLabel_Attribute_Exclusion_label;
	public static String ListLabel_Attribute_Inclusion_label;
	public static String ListLabel_Attribute_OutputFolder_label;
	public static String ListLabel_Attribute_NonModifiable_label;
	
	public static String ListLabel_Value_None_label;
	public static String ListLabel_Value_Filter_All_label;
	public static String ListLabel_Value_Filter_None_label;
	public static String ListLabel_Value_Path_separator;
	public static String ListLabel_Value_Output_Default_label;
	
	public static String ListLabel_Deco_New_label;
	public static String ListLabel_Deco_Missing_label;
	
	public static String SourceContainers_SourceFolders_label;
	public static String SourceContainers_OutputFolder_label;
	public static String SourceContainers_OutputFolder_Choose_label;
	public static String SourceContainers_OutputBySourceFolders_label;
	
	public static String ExclusionInclusion_Dialog_title;
	public static String ExclusionInclusion_Dialog_description;
	public static String ExclusionInclusion_InclusionPattern_label;
	public static String ExclusionInclusion_ExclusionPattern_label;
	public static String ExclusionInclusion_AddMulti_label;
	
	public static String ExclusionInclusion_EntryDialog_Exclude_Add_title;
	public static String ExclusionInclusion_EntryDialog_Exclude_Edit_title;
	public static String ExclusionInclusion_EntryDialog_Exclude_description;
	public static String ExclusionInclusion_EntryDialog_Include_Pattern_label;
	public static String ExclusionInclusion_EntryDialog_Include_Add_title;
	public static String ExclusionInclusion_EntryDialog_Include_Edit_title;
	public static String ExclusionInclusion_EntryDialog_Include_description;
	public static String ExclusionInclusion_EntryDialog_Exclude_Pattern_label;
	public static String ExclusionInclusion_EntryDialog_Choose_label;
	public static String ExclusionInclusion_EntryDialog_error_Empty_message;
	public static String ExclusionInclusion_EntryDialog_error_NotRelative_message;
	public static String ExclusionInclusion_EntryDialog_error_AlreadyExists_message;
	
	public static String ExclusionInclusion_Choose_Include_title;
	public static String ExclusionInclusion_Choose_Include_Single_description;
	public static String ExclusionInclusion_Choose_Include_Multi_description;
	public static String ExclusionInclusion_Choose_Exclude_title;
	public static String ExclusionInclusion_Choose_Exclude_Single_description;
	public static String ExclusionInclusion_Choose_Exclude_Multi_description;
	
	
	static {
		NLS.initializeMessages(Messages.class.getName(), Messages.class);
	}
	private Messages() {}
	
}
