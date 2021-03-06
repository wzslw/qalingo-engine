/**
 * Most of the code in the Qalingo project is copyrighted Hoteia and licensed
 * under the Apache License Version 2.0 (release version 0.8.0)
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *                   Copyright (c) Hoteia, 2012-2014
 * http://www.hoteia.com - http://twitter.com/hoteia - contact@hoteia.com
 *
 */
package org.hoteia.qalingo.core.pojo.market;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.hoteia.qalingo.core.domain.Localization;
import org.hoteia.qalingo.core.domain.Retailer;
import org.hoteia.qalingo.core.pojo.catalog.CatalogPojo;

public class MarketAreaPojo {

    private Long id;
    private int version;
    private String code;
    private String name;
    private String description;
    private boolean isDefault = false;
    private boolean isSelected = false;
    private String theme;
    
    private MarketPojo market;
    private CatalogPojo catalog;
    
    private Set<Localization> localizations = new HashSet<Localization>(); 
    
    private Set<Retailer> retailers = new HashSet<Retailer>(); 
    
    private Date dateCreate;
    private Date dateUpdate;

    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean isSelected) {
        this.isSelected = isSelected;
    }
    
    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }
    
    public MarketPojo getMarket() {
        return market;
    }
    
    public void setMarket(MarketPojo market) {
        this.market = market;
    }
    
    public CatalogPojo getCatalog() {
        return catalog;
    }
    
    public void setCatalog(CatalogPojo catalog) {
        this.catalog = catalog;
    }
    
    public Set<Localization> getLocalizations() {
        return localizations;
    }
    
    public void setLocalizations(Set<Localization> localizations) {
        this.localizations = localizations;
    }

    public Set<Retailer> getRetailers() {
        return retailers;
    }
    
    public void setRetailers(Set<Retailer> retailers) {
        this.retailers = retailers;
    }
    
    public Date getDateCreate() {
        return dateCreate;
    }

    public void setDateCreate(Date dateCreate) {
        this.dateCreate = dateCreate;
    }

    public Date getDateUpdate() {
        return dateUpdate;
    }

    public void setDateUpdate(Date dateUpdate) {
        this.dateUpdate = dateUpdate;
    }
    
}