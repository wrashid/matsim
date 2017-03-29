/* *********************************************************************** *
 * project: org.matsim.*
 * GtiLandCover.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

/**
 * 
 */
package playground.jjoubert.projects.wb.tiff;

/**
 * A land cover classification based on GeoTerraImage's 72 Class GTI South 
 * African National Land Cover Dataset (2013/2014).
 * 
 * @author jwjoubert
 */
public enum GtiLandCover {
	PERMANENT_WATER (1, "Permanent water"),
	SEASONAL_WATER (2, "Seasonal water"),
	WETLAND (3, "Wetland"),
	INDIGENOUS_FOREST (4, "Indigenous forest"),
	DENSE_BUSH (5, "Dense bush, thicket & tall dense shrubs"),
	WOODLAND (6, "Woodland and open bushland"),
	GRASSLAND (7, "Grassland"),
	LOW_SHRUBLAND_FYNBOS (8, "Low shrubland: Fynbos"),
	LOW_SHRUBLAND_OTHER (9, "Low shrubland: Other"),
	CULTIVATED_COMMERCIAL_ANNUAL_HIGH (10, "Commercial annuals (rainfed): NDVI profile high"),
	CULTIVATED_COMMERCIAL_ANNUAL_MED (11, "Commercial annuals (rainfed): NDVI profile med"),
	CULTIVATED_COMMERCIAL_ANNUAL_LOW (12, "Commercial annuals (rainfed): NDVI profile low"),
	CULTIVATED_COMMERCIAL_PIVOT_HIGH (13, "Commercial pivot: NDVI profile high"),
	CULTIVATED_COMMERCIAL_PIVOT_MED (14, "Commercial pivot: NDVI profile med"),
	CULTIVATED_COMMERCIAL_PIVOT_LOW (15, "Commercial pivot: NDVI profile low"),
	CULTIVATED_COMMERCIAL_ORCHARD_HIGH (16, "Commercial permanent orchards: NDVI profile high"),
	CULTIVATED_COMMERCIAL_ORCHARD_MED (17, "Commercial permanent orchards: NDVI profile med"),
	CULTIVATED_COMMERCIAL_ORCHARD_LOW (18, "Commercial permanent orchards: NDVI profile low"),
	CULTIVATED_COMMERCIAL_VINES_HIGH (19, "Commercial permanent viticulture: NDVI profile high"),
	CULTIVATED_COMMERCIAL_VINES_MED (20, "Commercial permanent viticulture: NDVI profile med"),
	CULTIVATED_COMMERCIAL_VINES_LOW (21, "Commercial permanent viticulture: NDVI profile low"),
	CULTIVATED_COMMERCIAL_PINE (22, "Commercial permanent pineapples"),
	CULTIVATED_SUBSISTENCE_HIGH (23, "Subsistence: NDVI profile high"),
	CULTIVATED_SUBSISTENCE_MED (24, "Subsistence: NDVI profile med"),
	CULTIVATED_SUBSISTENCE_LOW (25, "Subsistence: NDVI profile low"),
	CULTIVATED_SUGARCANE_PIVOT_STANDING (26, "Sugarcane pivot: standing crop"),
	CULTIVATED_SUGARCANE_PIVOT_TEMPORARY (27, "Sugarcane pivot: temporary fallow"),
	CULTIVATED_SUGARCANE_NONPIVOT_COMMERCIAL_STANDING (28, "Sugarcane non-pivot: commercial farmer standing crop"),
	CULTIVATED_SUGARCANE_NONPIVOT_COMMERCIAL_TEMPORARY (29, "Sugarcane non-pivot: comercial farmer temporary fallow"),
	CULTIVATED_SUGARCANE_NONPIVOT_EMERGING_STANDING (30, "Sugarcane non-pivot: emerging farmer standing crop"),
	CULTIVATED_SUGARCANE_NONPIVOT_EMERGING_TEMPORARY (31, "Sugarcane non-pivot: emerging farmer temporary fallow"),
	FOREST_MATURE (32, "Forest plantations: mature trees"),
	FOREST_YOUNG (33, "Forest plantations: young trees"),
	FOREST_CLEARFELLED (34, "Forest plantations: temporary clearfelled stands"),
	MINE_BARE (35, "Mine bare"),
	MINE_SEMIBARE (36, "Mine semi-bare"),
	MINE_WATER_SEASONAL (37, "Mine water seasonal"),
	MINE_WATER_PERMANENT (38, "Mine water permanent"),
	MINE_BUILDINGS (39, "Mine buildings"),
	BARE_EROSION (40, "Erosion dongas and gullies"),
	BARE_NONVEGTATED (41, "Bare (non vegetated)"),
	BUILTUP_COMMERCIAL (42, "Commercial"),
	BUILTUP_INDUSTRIAL (43, "Industrial"),
	BUILTUP_INFORMAL_TREE (44, "Informal (tree dominated)"),
	BUILTUP_INFORMAL_BUSH (45, "Informal (bush dominated)"),
	BUILTUP_INFORMAL_GRASS (46, "Informal (grass dominated)"),
	BUILTUP_INFORMAL_BARE (47, "Informal (bare dominated)"),
	BUILTUP_RESIDENTIAL_TREE (48, "Residential (tree dominated)"),
	BUILTUP_RESIDENTIAL_BUSH (49, "Residential (bush dominated)"),
	BUILTUP_RESIDENTIAL_GRASS (50, "Residential (grass dominated)"),
	BUILTUP_RESIDENTIAL_BARE (51, "Residential (bare dominated)"),
	BUILTUP_SCHOOL (52, "Schools & sport grounds"),
	BUILTUP_SMALLHOLDING_TREE (53, "Smallholding (tree dominated)"),
	BUILTUP_SMALLHOLDING_BUSH (54, "Smallholding (bush dominated)"),
	BUILTUP_SMALLHOLDING_GRASS (55, "Smallholding (grass dominated)"),
	BUILTUP_SMALLHOLDING_BARE (56, "Smallholding (bare dominated)"),
	BUILTUP_SPORT_TREE (57, "Sports & golf (tree dominated)"),
	BUILTUP_SPORT_BUSH (58, "Sports & golf (bush dominated)"),
	BUILTUP_SPORT_GRASS (59, "Sports & golf (grass dominated)"),
	BUILTUP_SPORT_BARE (60, "Sports & golf (bare dominated)"),
	BUILTUP_TOWNSHIP_TREE (61, "Township (tree dominated)"),
	BUILTUP_TOWNSHIP_BUSH (62, "Township (bush dominated)"),
	BUILTUP_TOWNSHIP_GRASS (63, "Township (grass dominated)"),
	BUILTUP_TOWNSHIP_BARE (64, "Township (bare dominated)"),
	BUILTUP_VILLAGE_TREE (65, "Village (tree dominated)"),
	BUILTUP_VILLAGE_BUSH (66, "Village (bush dominated)"),
	BUILTUP_VILLAGE_GRASS (67, "Village (grass dominated)"),
	BUILTUP_VILLAGE_BARE (68, "Village (bare dominated)"),
	BUILTUP_OTHER_TREE (69, "Built-up (tree dominated)"),
	BUILTUP_OTHER_BUSH (70, "Built-up (bush dominated)"),
	BUILTUP_OTHER_GRASS (71, "Built-up (grass dominated)"),
	BUILTUP_OTHER_BARE (72, "Built-up (bare dominated)")
	;
	
	private final int code;
	private final String description;
	
	private GtiLandCover(int code, String descr) {
		this.code = code;
		this.description = descr;
	}
	
	public int getClassCode(){
		return this.code;
	}
	
	public String getDescription(){
		return this.description;
	}
	
	public boolean isBuiltUp(int code){
		return code >= 42 && code <= 72 ? true : false;
	}
	
	
}
