/**
 * Most of the code in the Qalingo project is copyrighted Hoteia and licensed
 * under the Apache License Version 2.0 (release version 0.8.0)
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *                   Copyright (c) Hoteia, 2012-2014
 * http://www.hoteia.com - http://twitter.com/hoteia - contact@hoteia.com
 *
 */
package org.hoteia.qalingo.core.web.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.hoteia.qalingo.core.Constants;
import org.hoteia.qalingo.core.RequestConstants;
import org.hoteia.qalingo.core.domain.AbstractEngineSession;
import org.hoteia.qalingo.core.domain.Cart;
import org.hoteia.qalingo.core.domain.Company;
import org.hoteia.qalingo.core.domain.CurrencyReferential;
import org.hoteia.qalingo.core.domain.Customer;
import org.hoteia.qalingo.core.domain.EngineBoSession;
import org.hoteia.qalingo.core.domain.EngineEcoSession;
import org.hoteia.qalingo.core.domain.EngineSetting;
import org.hoteia.qalingo.core.domain.EngineSettingValue;
import org.hoteia.qalingo.core.domain.GeolocAddress;
import org.hoteia.qalingo.core.domain.GeolocCity;
import org.hoteia.qalingo.core.domain.Localization;
import org.hoteia.qalingo.core.domain.Market;
import org.hoteia.qalingo.core.domain.MarketArea;
import org.hoteia.qalingo.core.domain.MarketPlace;
import org.hoteia.qalingo.core.domain.OrderCustomer;
import org.hoteia.qalingo.core.domain.Retailer;
import org.hoteia.qalingo.core.domain.User;
import org.hoteia.qalingo.core.domain.bean.GeolocData;
import org.hoteia.qalingo.core.domain.bean.GeolocDataCity;
import org.hoteia.qalingo.core.domain.bean.GeolocDataCountry;
import org.hoteia.qalingo.core.domain.enumtype.EnvironmentType;
import org.hoteia.qalingo.core.domain.enumtype.FoUrls;
import org.hoteia.qalingo.core.fetchplan.customer.FetchPlanGraphCustomer;
import org.hoteia.qalingo.core.i18n.enumtype.ScopeCommonMessage;
import org.hoteia.qalingo.core.i18n.message.CoreMessageSource;
import org.hoteia.qalingo.core.pojo.RequestData;
import org.hoteia.qalingo.core.pojo.UrlParameterMapping;
import org.hoteia.qalingo.core.service.CartService;
import org.hoteia.qalingo.core.service.CatalogCategoryService;
import org.hoteia.qalingo.core.service.CurrencyReferentialService;
import org.hoteia.qalingo.core.service.CustomerService;
import org.hoteia.qalingo.core.service.EngineSessionService;
import org.hoteia.qalingo.core.service.EngineSettingService;
import org.hoteia.qalingo.core.service.GeolocService;
import org.hoteia.qalingo.core.service.LocalizationService;
import org.hoteia.qalingo.core.service.MarketService;
import org.hoteia.qalingo.core.service.ProductService;
import org.hoteia.qalingo.core.service.ReferentialDataService;
import org.hoteia.qalingo.core.service.RetailerService;
import org.hoteia.qalingo.core.service.UserService;
import org.hoteia.qalingo.core.web.bean.clickstream.ClickstreamRequest;
import org.hoteia.qalingo.core.web.bean.clickstream.ClickstreamSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 * <a href="RequestUtilImpl.java.html"><i>View Source</i></a>
 * </p>
 * 
 * @author Denis Gosset <a href="http://www.hoteia.com"><i>Hoteia.com</i></a>
 * 
 */
@Service("requestUtil")
@Transactional
public class RequestUtil {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${env.name}")
    protected String environmentName;

    @Value("${app.name}")
    protected String applicationName;

    @Value("${context.name}")
    protected String contextName;

    @Value("${cookie.prefix}")
    protected String cookiePrefix;
    
    @Autowired
    protected MarketService marketService;

    @Autowired
    protected CatalogCategoryService catalogCategoryService;
    
    @Autowired
    protected ProductService productService;

    @Autowired
    protected CustomerService customerService;

    @Autowired
    protected LocalizationService localizationService;
    
    @Autowired
    protected RetailerService retailerService;
    
    @Autowired
    protected CurrencyReferentialService currencyReferentialService;
    
    @Autowired
    protected ReferentialDataService referentialDataService;
    
    @Autowired
    protected UserService userService;
    
    @Autowired
    protected EngineSettingService engineSettingService;
    
    @Autowired
    protected EngineSessionService engineSessionService;
    
    @Autowired
    protected CartService cartService;
    
    @Autowired
    protected GeolocService geolocService;
    
    @Autowired
    protected CoreMessageSource coreMessageSource;
    
    /**
     * 
     */
    public void handleFrontofficeUrlParameters(final HttpServletRequest request) throws Exception {
        UrlParameterMapping urlParameterMapping = handleUrlParameters(request);
        String marketPlaceCode = urlParameterMapping.getMarketPlaceCode();
        String marketCode = urlParameterMapping.getMarketCode();
        String marketAreaCode = urlParameterMapping.getMarketAreaCode();
        String localizationCode = urlParameterMapping.getLocalizationCode();
        String retailerCode = urlParameterMapping.getRetailerCode();
        String currencyCode = urlParameterMapping.getCurrencyCode();

        EngineEcoSession engineEcoSession = getCurrentEcoSession(request);

        engineEcoSession = checkEngineEcoSession(request);

        if (StringUtils.isNotEmpty(marketPlaceCode) && StringUtils.isNotEmpty(marketCode) && StringUtils.isNotEmpty(marketAreaCode) && StringUtils.isNotEmpty(localizationCode)) {
            MarketPlace currentMarketPlace = engineEcoSession.getCurrentMarketPlace();
            if (currentMarketPlace != null && !currentMarketPlace.getCode().equalsIgnoreCase(marketPlaceCode)) {
                // RESET ALL SESSION AND CHANGE THE MARKET PLACE
                initEcoSession(request);
                MarketPlace newMarketPlace = marketService.getMarketPlaceByCode(marketPlaceCode);
                if (newMarketPlace == null) {
                    // INIT A DEFAULT MARKET PLACE
                    initEcoMarketPlace(request);
                } else {
                    // MARKET PLACE
                    engineEcoSession = (EngineEcoSession) setSessionMarketPlace(engineEcoSession, newMarketPlace);
                    updateCurrentTheme(request, newMarketPlace.getTheme());

                    // MARKET
                    Market market = newMarketPlace.getMarket(marketCode);
                    if (market == null) {
                        market = newMarketPlace.getDefaultMarket();
                    }
                    engineEcoSession = (EngineEcoSession) setSessionMarket(engineEcoSession, market);

                    // MARKET AREA
                    MarketArea marketArea = market.getMarketArea(marketAreaCode);
                    if (marketArea == null) {
                        marketArea = market.getDefaultMarketArea();
                    }
                    engineEcoSession = (EngineEcoSession) setSessionMarketArea(engineEcoSession, marketArea);
                    marketArea = engineEcoSession.getCurrentMarketArea();

                    // LOCALE
                    Localization localization = marketArea.getLocalization(localizationCode);
                    if (localization == null) {
                        Localization defaultLocalization = marketArea.getDefaultLocalization();
                        engineEcoSession = (EngineEcoSession) setSessionMarketAreaLocalization(engineEcoSession, defaultLocalization);
                    } else {
                        engineEcoSession = (EngineEcoSession) setSessionMarketAreaLocalization(engineEcoSession, localization);
                    }

                    // RETAILER
                    Retailer retailer = marketArea.getRetailer(retailerCode);
                    if (retailer == null) {
                        Retailer defaultRetailer = marketArea.getDefaultRetailer();
                        engineEcoSession = (EngineEcoSession) setSessionMarketAreaRetailer(engineEcoSession, defaultRetailer);
                    } else {
                        engineEcoSession = (EngineEcoSession) setSessionMarketAreaRetailer(engineEcoSession, retailer);
                    }

                    // CURRENCY
                    CurrencyReferential currency = marketArea.getCurrency(currencyCode);
                    if (currency == null) {
                        CurrencyReferential defaultCurrency = marketArea.getDefaultCurrency();
                        engineEcoSession = (EngineEcoSession) setSessionMarketAreaCurrency(engineEcoSession, defaultCurrency);
                    } else {
                        engineEcoSession = (EngineEcoSession) setSessionMarketAreaCurrency(engineEcoSession, currency);
                    }
                }

            } else {
                Market market = engineEcoSession.getCurrentMarket();
                if (market != null && !market.getCode().equalsIgnoreCase(marketCode)) {

                    // CHANGE THE MARKET
                    Market newMarket = marketService.getMarketByCode(marketCode);
                    if (newMarket == null) {
                        newMarket = currentMarketPlace.getDefaultMarket();
                    }
                    engineEcoSession = (EngineEcoSession) setSessionMarket(engineEcoSession, market);
                    updateCurrentTheme(request, newMarket.getTheme());

                    // MARKET AREA
                    MarketArea marketArea = newMarket.getMarketArea(marketAreaCode);
                    if (marketArea == null) {
                        marketArea = market.getDefaultMarketArea();
                    }
                    engineEcoSession = (EngineEcoSession) setSessionMarketArea(engineEcoSession, marketArea);
                    marketArea = engineEcoSession.getCurrentMarketArea();

                    // LOCALE
                    Localization localization = marketArea.getLocalization(localizationCode);
                    if (localization == null) {
                        Localization defaultLocalization = marketArea.getDefaultLocalization();
                        engineEcoSession = (EngineEcoSession) setSessionMarketAreaLocalization(engineEcoSession, defaultLocalization);
                    } else {
                        engineEcoSession = (EngineEcoSession) setSessionMarketAreaLocalization(engineEcoSession, localization);
                    }

                    // RETAILER
                    Retailer retailer = marketArea.getRetailer(retailerCode);
                    if (retailer == null) {
                        Retailer defaultRetailer = marketArea.getDefaultRetailer();
                        engineEcoSession = (EngineEcoSession) setSessionMarketAreaRetailer(engineEcoSession, defaultRetailer);
                    } else {
                        engineEcoSession = (EngineEcoSession) setSessionMarketAreaRetailer(engineEcoSession, retailer);
                    }

                    // CURRENCY
                    CurrencyReferential currency = marketArea.getCurrency(currencyCode);
                    if (currency == null) {
                        CurrencyReferential defaultCurrency = marketArea.getDefaultCurrency();
                        engineEcoSession = (EngineEcoSession) setSessionMarketAreaCurrency(engineEcoSession, defaultCurrency);
                    } else {
                        engineEcoSession = (EngineEcoSession) setSessionMarketAreaCurrency(engineEcoSession, currency);
                    }

                } else {
                    MarketArea marketArea = engineEcoSession.getCurrentMarketArea();
                    if (marketArea != null && !marketArea.getCode().equalsIgnoreCase(marketAreaCode)) {

                        // CHANGE THE MARKET AREA
                        MarketArea newMarketArea = market.getMarketArea(marketAreaCode);
                        if (newMarketArea == null) {
                            newMarketArea = market.getDefaultMarketArea();
                        }
                        engineEcoSession = (EngineEcoSession) setSessionMarketArea(engineEcoSession, newMarketArea);
                        marketArea = engineEcoSession.getCurrentMarketArea();
                        updateCurrentTheme(request, newMarketArea.getTheme());

                        // LOCALE
                        Localization localization = newMarketArea.getLocalization(localizationCode);
                        if (localization == null) {
                            Localization defaultLocalization = marketArea.getDefaultLocalization();
                            engineEcoSession = (EngineEcoSession) setSessionMarketAreaLocalization(engineEcoSession, defaultLocalization);
                        } else {
                            engineEcoSession = (EngineEcoSession) setSessionMarketAreaLocalization(engineEcoSession, localization);
                        }

                        // RETAILER
                        Retailer retailer = marketArea.getRetailer(retailerCode);
                        if (retailer == null) {
                            Retailer defaultRetailer = marketArea.getDefaultRetailer();
                            engineEcoSession = (EngineEcoSession) setSessionMarketAreaRetailer(engineEcoSession, defaultRetailer);
                        } else {
                            engineEcoSession = (EngineEcoSession) setSessionMarketAreaRetailer(engineEcoSession, retailer);
                        }

                        // CURRENCY
                        CurrencyReferential currency = marketArea.getCurrency(currencyCode);
                        if (currency == null) {
                            CurrencyReferential defaultCurrency = marketArea.getDefaultCurrency();
                            engineEcoSession = (EngineEcoSession) setSessionMarketAreaCurrency(engineEcoSession, defaultCurrency);
                        } else {
                            engineEcoSession = (EngineEcoSession) setSessionMarketAreaCurrency(engineEcoSession, currency);
                        }

                    } else {
                        Localization localization = engineEcoSession.getCurrentMarketAreaLocalization();
                        Retailer retailer = engineEcoSession.getCurrentMarketAreaRetailer();
                        CurrencyReferential currency = engineEcoSession.getCurrentMarketAreaCurrency();
                        if (localization != null && !localization.getLocale().toString().equalsIgnoreCase(localizationCode)) {
                            // CHANGE THE LOCALE
                            Localization newLocalization = marketArea.getLocalization(localizationCode);
                            if (newLocalization == null) {
                                Localization defaultLocalization = marketArea.getDefaultLocalization();
                                engineEcoSession = (EngineEcoSession) setSessionMarketAreaLocalization(engineEcoSession, defaultLocalization);
                            } else {
                                engineEcoSession = (EngineEcoSession) setSessionMarketAreaLocalization(engineEcoSession, newLocalization);
                            }

                        } else if (retailer != null && !retailer.getCode().toString().equalsIgnoreCase(localizationCode)) {
                            // CHANGE THE RETAILER
                            Retailer newRetailer = marketArea.getRetailer(retailerCode);
                            if (newRetailer == null) {
                                Retailer defaultRetailer = marketArea.getDefaultRetailer();
                                engineEcoSession = (EngineEcoSession) setSessionMarketAreaRetailer(engineEcoSession, defaultRetailer);
                            } else {
                                engineEcoSession = (EngineEcoSession) setSessionMarketAreaRetailer(engineEcoSession, newRetailer);
                            }
                            
                        } else if (currency != null && !currency.getCode().toString().equalsIgnoreCase(currencyCode)) {
                            // CHANGE THE CURRENCY
                            CurrencyReferential newCurrency = marketArea.getCurrency(currencyCode);
                            if (newCurrency == null) {
                                CurrencyReferential defaultCurrency = marketArea.getDefaultCurrency();
                                engineEcoSession = (EngineEcoSession) setSessionMarketAreaCurrency(engineEcoSession, defaultCurrency);
                            } else {
                                engineEcoSession = (EngineEcoSession) setSessionMarketAreaCurrency(engineEcoSession, newCurrency);
                            }
                        }
                    }
                }
            }
        }

        // THEME
        final MarketArea marketArea = engineEcoSession.getCurrentMarketArea();
        String themeFolder = "default";
        if (StringUtils.isNotEmpty(marketArea.getTheme())) {
            themeFolder = marketArea.getTheme();
        }
        updateCurrentTheme(request, themeFolder);

        // SAVE THE ENGINE SESSION
        updateCurrentEcoSession(request, engineEcoSession);
    }

    /**
    * 
    */
    public void handleBackofficeUrlParameters(final HttpServletRequest request) throws Exception {
        UrlParameterMapping urlParameterMapping = handleUrlParameters(request);
        String marketPlaceCode = urlParameterMapping.getMarketPlaceCode();
        String marketCode = urlParameterMapping.getMarketCode();
        String marketAreaCode = urlParameterMapping.getMarketAreaCode();
        String localizationCode = urlParameterMapping.getLocalizationCode();
        String retailerCode = urlParameterMapping.getRetailerCode();
        String currencyCode = urlParameterMapping.getCurrencyCode();

        EngineBoSession engineBoSession = getCurrentBoSession(request);

        engineBoSession = checkEngineBoSession(request);
        
        MarketPlace currentMarketPlace = engineBoSession.getCurrentMarketPlace();
        if (StringUtils.isNotEmpty(marketPlaceCode) && StringUtils.isNotEmpty(marketCode) && StringUtils.isNotEmpty(marketAreaCode) && StringUtils.isNotEmpty(localizationCode)) {
            if (currentMarketPlace != null && !currentMarketPlace.getCode().equalsIgnoreCase(marketPlaceCode)) {
                // RESET ALL SESSION AND CHANGE THE MARKET PLACE
                initBoSession(request);
                MarketPlace newMarketPlace = marketService.getMarketPlaceByCode(marketPlaceCode);
                if (newMarketPlace == null) {
                    // INIT A DEFAULT MARKET PLACE
                    initDefaultBoMarketPlace(request);
                } else {
                    // MARKET PLACE
                    engineBoSession = (EngineBoSession) setSessionMarketPlace(engineBoSession, newMarketPlace);
                    updateCurrentTheme(request, newMarketPlace.getTheme());

                    // MARKET
                    Market market = newMarketPlace.getMarket(marketCode);
                    if (market == null) {
                        market = newMarketPlace.getDefaultMarket();
                    }
                    engineBoSession = (EngineBoSession) setSessionMarket(engineBoSession, market);

                    // MARKET AREA
                    MarketArea marketArea = market.getMarketArea(marketAreaCode);
                    if (marketArea == null) {
                        marketArea = market.getDefaultMarketArea();
                    }
                    engineBoSession = (EngineBoSession) setSessionMarketArea(engineBoSession, marketArea);

                    // LOCALE
                    Localization localization = marketArea.getLocalization(localizationCode);
                    if (localization == null) {
                        Localization defaultLocalization = marketArea.getDefaultLocalization();
                        engineBoSession = (EngineBoSession) setSessionMarketAreaLocalization(engineBoSession, defaultLocalization);
                    } else {
                        engineBoSession = (EngineBoSession) setSessionMarketAreaLocalization(engineBoSession, localization);
                    }

                    // RETAILER
                    Retailer retailer = marketArea.getRetailer(localizationCode);
                    if (retailer == null) {
                        Retailer defaultRetailer = marketArea.getDefaultRetailer();
                        engineBoSession = (EngineBoSession) setSessionMarketAreaRetailer(engineBoSession, defaultRetailer);
                    } else {
                        engineBoSession = (EngineBoSession) setSessionMarketAreaRetailer(engineBoSession, retailer);
                    }
                    
                    // CURRENCY
                    CurrencyReferential currency = marketArea.getCurrency(currencyCode);
                    if (currency == null) {
                        CurrencyReferential defaultCurrency = marketArea.getDefaultCurrency();
                        engineBoSession = (EngineBoSession) setSessionMarketAreaCurrency(engineBoSession, defaultCurrency);
                    } else {
                        engineBoSession = (EngineBoSession) setSessionMarketAreaCurrency(engineBoSession, currency);
                    }

                }

            } else {
                Market market = engineBoSession.getCurrentMarket();
                if (market != null && !market.getCode().equalsIgnoreCase(marketCode)) {

                    // CHANGE THE MARKET
                    Market newMarket = marketService.getMarketByCode(marketCode);
                    if (newMarket == null) {
                        newMarket = currentMarketPlace.getDefaultMarket();
                    }
                    engineBoSession = (EngineBoSession) setSessionMarket(engineBoSession, market);
                    updateCurrentTheme(request, newMarket.getTheme());

                    // MARKET AREA
                    MarketArea marketArea = newMarket.getMarketArea(marketAreaCode);
                    if (marketArea == null) {
                        marketArea = market.getDefaultMarketArea();
                    }
                    engineBoSession = (EngineBoSession) setSessionMarketArea(engineBoSession, marketArea);

                    // LOCALE
                    Localization localization = marketArea.getLocalization(localizationCode);
                    if (localization == null) {
                        Localization defaultLocalization = marketArea.getDefaultLocalization();
                        engineBoSession = (EngineBoSession) setSessionMarketAreaLocalization(engineBoSession, defaultLocalization);
                    } else {
                        engineBoSession = (EngineBoSession) setSessionMarketAreaLocalization(engineBoSession, localization);
                    }

                    // RETAILER
                    Retailer retailer = marketArea.getRetailer(retailerCode);
                    if (retailer == null) {
                        Retailer defaultRetailer = marketArea.getDefaultRetailer();
                        engineBoSession = (EngineBoSession) setSessionMarketAreaRetailer(engineBoSession, defaultRetailer);
                    } else {
                        engineBoSession = (EngineBoSession) setSessionMarketAreaRetailer(engineBoSession, retailer);
                    }
                    
                    // CURRENCY
                    CurrencyReferential currency = marketArea.getCurrency(currencyCode);
                    if (currency == null) {
                        CurrencyReferential defaultCurrency = marketArea.getDefaultCurrency();
                        engineBoSession = (EngineBoSession) setSessionMarketAreaCurrency(engineBoSession, defaultCurrency);
                    } else {
                        engineBoSession = (EngineBoSession) setSessionMarketAreaCurrency(engineBoSession, currency);
                    }

                } else {
                    MarketArea marketArea = engineBoSession.getCurrentMarketArea();
                    if (marketArea != null && !marketArea.getCode().equalsIgnoreCase(marketAreaCode)) {

                        // CHANGE THE MARKET AREA
                        MarketArea newMarketArea = market.getMarketArea(marketAreaCode);
                        if (newMarketArea == null) {
                            newMarketArea = market.getDefaultMarketArea();
                        }
                        engineBoSession = (EngineBoSession) setSessionMarketArea(engineBoSession, marketArea);
                        updateCurrentTheme(request, newMarketArea.getTheme());

                        // LOCALE
                        Localization localization = newMarketArea.getLocalization(localizationCode);
                        if (localization == null) {
                            Localization defaultLocalization = marketArea.getDefaultLocalization();
                            engineBoSession = (EngineBoSession) setSessionMarketAreaLocalization(engineBoSession, defaultLocalization);
                        } else {
                            engineBoSession = (EngineBoSession) setSessionMarketAreaLocalization(engineBoSession, localization);
                        }

                        // RETAILER
                        Retailer retailer = marketArea.getRetailer(retailerCode);
                        if (retailer == null) {
                            Retailer defaultRetailer = marketArea.getDefaultRetailer();
                            engineBoSession = (EngineBoSession) setSessionMarketAreaRetailer(engineBoSession, defaultRetailer);
                        } else {
                            engineBoSession = (EngineBoSession) setSessionMarketAreaRetailer(engineBoSession, retailer);
                        }
                        
                        // CURRENCY
                        CurrencyReferential currency = marketArea.getCurrency(currencyCode);
                        if (currency == null) {
                            CurrencyReferential defaultCurrency = marketArea.getDefaultCurrency();
                            engineBoSession = (EngineBoSession) setSessionMarketAreaCurrency(engineBoSession, defaultCurrency);
                        } else {
                            engineBoSession = (EngineBoSession) setSessionMarketAreaCurrency(engineBoSession, currency);
                        }

                    } else {
                        Localization localization = engineBoSession.getCurrentMarketAreaLocalization();
                        Retailer retailer = engineBoSession.getCurrentMarketAreaRetailer();
                        CurrencyReferential currency = engineBoSession.getCurrentMarketAreaCurrency();
                        if (localization != null && !localization.getLocale().toString().equalsIgnoreCase(localizationCode)) {
                            // CHANGE THE LOCALE
                            Localization newLocalization = marketArea.getLocalization(localizationCode);
                            if (newLocalization == null) {
                                Localization defaultLocalization = marketArea.getDefaultLocalization();
                                engineBoSession = (EngineBoSession) setSessionMarketAreaLocalization(engineBoSession, defaultLocalization);
                            } else {
                                engineBoSession = (EngineBoSession) setSessionMarketAreaLocalization(engineBoSession, newLocalization);
                            }

                        } else if (retailer != null && !retailer.getCode().toString().equalsIgnoreCase(localizationCode)) {
                            // CHANGE THE RETAILER
                            Retailer newRetailer = marketArea.getRetailer(retailerCode);
                            if (newRetailer == null) {
                                Retailer defaultRetailer = marketArea.getDefaultRetailer();
                                engineBoSession = (EngineBoSession) setSessionMarketAreaRetailer(engineBoSession, defaultRetailer);
                            } else {
                                engineBoSession = (EngineBoSession) setSessionMarketAreaRetailer(engineBoSession, newRetailer);
                            }
                            
                        } else if (currency != null && !currency.getCode().toString().equalsIgnoreCase(currencyCode)) {
                            // CHANGE THE CURRENCY
                            CurrencyReferential newCurrency = marketArea.getCurrency(currencyCode);
                            if (newCurrency == null) {
                                CurrencyReferential defaultCurrency = marketArea.getDefaultCurrency();
                                engineBoSession = (EngineBoSession) setSessionMarketAreaCurrency(engineBoSession, defaultCurrency);
                            } else {
                                engineBoSession = (EngineBoSession) setSessionMarketAreaCurrency(engineBoSession, newCurrency);
                            }
                        }
                    }
                }
            }
        }

        // CHECK BACKOFFICE LANGUAGES
        String backofficeLocalizationCode = request.getParameter(RequestConstants.REQUEST_PARAMETER_LOCALE_CODE);
        Localization backofficeLocalization = engineBoSession.getCurrentBackofficeLocalization();
        if(StringUtils.isNotEmpty(backofficeLocalizationCode)){
            Company company = getCurrentCompany(request);
            if (company != null) {
                backofficeLocalization = company.getLocalization(backofficeLocalizationCode);
            } else {
                backofficeLocalization = localizationService.getLocalizationByCode(backofficeLocalizationCode);
            }
        } else {
            String requestLocale = request.getLocale().toString();
            if(backofficeLocalization == null 
            		|| (backofficeLocalization != null && "en".equalsIgnoreCase(backofficeLocalization.getCode()))
                    && StringUtils.isNotEmpty(requestLocale)){
                if (requestLocale.length() > 2) {
                    String localeLanguage = request.getLocale().getLanguage();
                    backofficeLocalization = localizationService.getLocalizationByCode(localeLanguage);
                } else if (requestLocale.length() == 2) {
                    backofficeLocalization = localizationService.getLocalizationByCode(requestLocale);
                }
            }
        }
        if (backofficeLocalization == null) {
            // FALLBACK LOCALE EN
            backofficeLocalization = localizationService.getLocalizationByCode("en");
        }
        engineBoSession.setCurrentBackofficeLocalization(backofficeLocalization);

        // SAVE THE ENGINE SESSION
        updateCurrentBoSession(request, engineBoSession);
    }
   
    /**
	 *
	 */
    public boolean isLocalHostMode(final HttpServletRequest request) throws Exception {
        if (StringUtils.isNotEmpty(getHost(request)) && (getHost(request).contains("localhost") || getHost(request).equalsIgnoreCase("127.0.0.1"))) {
            return true;
        }
        return false;
    }

    /**
	 *
	 */
    public String getHost(final HttpServletRequest request) throws Exception {
        return (String) request.getHeader(Constants.HOST);
    }

    /**
     * 
     */
    public String getRemoteAddr(final HttpServletRequest request){
        String customerRemoteAddr = request.getRemoteAddr();
        String xForwardedFor = request.getHeader(Constants.X_FORWARDED_FOR);        
        if(StringUtils.isNotEmpty(xForwardedFor)){
            customerRemoteAddr = xForwardedFor;
            if(xForwardedFor.contains(",")){
                // THERE IS MANY IP ADDRESS WHICH MEAN MANY PROXY
                String[] xForwardedForList = xForwardedFor.split(",");
                customerRemoteAddr = xForwardedForList[0];
            }
        }
        return customerRemoteAddr;
    }
    
    /**
	 *
	 */
    public String getEnvironmentName() throws Exception {
        return environmentName;
    }

    /**
	 *
	 */
    public String getApplicationName() throws Exception {
        return applicationName;
    }

    /**
	 *
	 */
    public String getContextName() throws Exception {
        return contextName;
    }

    /**
	 *
	 */
    public DateFormat getCommonFormatDate(final RequestData requestData, final Integer dateStyle, final Integer timeStyle) throws Exception {
        final Locale locale = requestData.getLocale();
        DateFormat formatter = null;
        if(timeStyle != null){
            formatter = DateFormat.getDateTimeInstance(dateStyle, timeStyle, locale);
        } else {
            formatter = DateFormat.getDateInstance(dateStyle, locale);
        }
        return formatter;
    }

    /**
	 *
	 */
    public SimpleDateFormat getRssFormatDate(final RequestData requestData) throws Exception {
        SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
        return formatter;
    }

    /**
     *
     */
   public String getFormatDatePattern(final RequestData requestData) throws Exception {
       if(requestData.getMarketAreaLocalization() != null && StringUtils.isNotEmpty(requestData.getMarketAreaLocalization().getFormatDatePattern())){
           return requestData.getMarketAreaLocalization().getFormatDatePattern();
       } else {
           return "yyyy-MM-dd";
       }
   }
   
   /**
    *
    */
  public SimpleDateFormat getFormatDate(final RequestData requestData) throws Exception {
      SimpleDateFormat formatter = new SimpleDateFormat(getFormatDatePattern(requestData));
      return formatter;
  }
   
    /**
	 *
	 */
    public SimpleDateFormat getDataVocabularyFormatDate(final RequestData requestData) throws Exception {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        return formatter;
    }

    /**
	 *
	 */
    public SimpleDateFormat getAtomFormatDate(final RequestData requestData) throws Exception {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz");
        return formatter;
    }

    /**
     * 
     */
    public ClickstreamSession getClickstreamSession(final HttpServletRequest request) throws Exception {
        ClickstreamSession clickstream = (ClickstreamSession) request.getSession().getAttribute(Constants.ENGINE_CLICKSTREAM);
        if(clickstream == null){
            clickstream = new ClickstreamSession();
            clickstream.setInitialReferrer(request.getHeader("REFERER"));
        }
        return clickstream;
    }
    
    /**
     * 
     */
    public void addClickstream(final HttpServletRequest request) throws Exception {
        ClickstreamSession clickstream = getClickstreamSession(request);
        Date lastRequest = new Date();
        clickstream.setLastRequest(lastRequest);
        clickstream.setHostname(request.getRemoteHost());
        
        clickstream.getRequests().add(new ClickstreamRequest(request, lastRequest));

        request.getSession().setAttribute(Constants.ENGINE_CLICKSTREAM, clickstream);
    }
    
    /**
     * 
     */
    public String getLastRequestUrlNotSecurity(final HttpServletRequest request) throws Exception {
        final List<String> excludedPatterns = getCommonUrlExcludedPatterns();
        return getRequestUrl(request, excludedPatterns, 1);
    }

    /**
     * 
     */
    public String getRequestUrlAfterChangeContext(final HttpServletRequest request, final String fallbackUrl) throws Exception {
        final List<String> excludedPatterns = getCommonUrlExcludedPatterns();
        String currentUrl = getRequestUrl(request, excludedPatterns, 0);
        if(StringUtils.isNotEmpty(currentUrl)){
            return currentUrl;
        }
        return fallbackUrl;
    }
    
    /**
     * 
     */
    public String getCurrentRequestUrl(final HttpServletRequest request, final List<String> excludedPatterns) throws Exception {
        return getRequestUrl(request, excludedPatterns, 0);
    }

    /**
     * 
     */
    public String getCurrentRequestUrl(final HttpServletRequest request, final String fallbackUrl) throws Exception {
        String currentUrl = getRequestUrl(request, new ArrayList<String>(), 0);
        if(StringUtils.isNotEmpty(currentUrl)){
            return currentUrl;
        }
        return fallbackUrl;
    }
    
    /**
     * 
     */
    public String getCurrentRequestUrl(final HttpServletRequest request) throws Exception {
        return getRequestUrl(request, new ArrayList<String>(), 0);
    }

    /**
     * 
     */
    public String getCurrentRequestUrlNotSecurity(final HttpServletRequest request) throws Exception {
        final List<String> excludedPatterns = getCommonUrlExcludedPatterns();
        return getRequestUrl(request, excludedPatterns, 0);
    }

    /**
     * 
     */
    public String getLastRequestForEmptyCartUrl(final HttpServletRequest request, final String fallbackUrl) throws Exception {
        final List<String> excludedPatterns = getCommonUrlExcludedPatterns();
        excludedPatterns.add("cart");
        String lastUrl = getLastRequestUrl(request, excludedPatterns, fallbackUrl);
        return lastUrl;
    }
    
    /**
     * 
     */
    public List<String> getCommonUrlExcludedPatterns() throws Exception {
        final List<String> excludedPatterns = new ArrayList<String>();
        excludedPatterns.add(FoUrls.ERROR_400.getUrlPatternKey());
        excludedPatterns.add(FoUrls.ERROR_403.getUrlPatternKey());
        excludedPatterns.add(FoUrls.ERROR_404.getUrlPatternKey());
        excludedPatterns.add(FoUrls.ERROR_500.getUrlPatternKey());
        excludedPatterns.add(FoUrls.CHANGE_CONTEXT.getUrlPatternKey());
        excludedPatterns.add(FoUrls.CHANGE_LANGUAGE.getUrlPatternKey());
        excludedPatterns.add(FoUrls.FORBIDDEN.getUrlPatternKey());
        excludedPatterns.add(FoUrls.LOGIN.getUrlPatternKey());
        excludedPatterns.add(FoUrls.LOGOUT.getUrlPatternKey());
        excludedPatterns.add(FoUrls.TIMEOUT.getUrlPatternKey());
        excludedPatterns.add(FoUrls.FORGOTTEN_PASSWORD.getUrlPatternKey());
        excludedPatterns.add(FoUrls.RESET_PASSWORD.getUrlPatternKey());
        excludedPatterns.add(FoUrls.CANCEL_RESET_PASSWORD.getUrlPatternKey());
        excludedPatterns.add("cachemanager.html");
        return excludedPatterns;
    }
    
    /**
     * 
     */
    public String getLastRequestUrl(final HttpServletRequest request, String fallbackUrl) throws Exception {
        return getLastRequestUrl(request, new ArrayList<String>(), fallbackUrl);
    }
    
    /**
     * 
     */
    public String getLastRequestUrl(final HttpServletRequest request, final List<String> moreExcludedPatterns, String fallbackUrl) throws Exception {
        final List<String> excludedPatterns = getCommonUrlExcludedPatterns();
        excludedPatterns.addAll(moreExcludedPatterns);
        String url = getRequestUrl(request, excludedPatterns, 1);
        if (StringUtils.isEmpty(url)) {
            return fallbackUrl;
        }
        return url;
    }

    /**
     * 
     */
    public String getLastRequestUrl(final HttpServletRequest request, final List<String> excludedPatterns) throws Exception {
        return getRequestUrl(request, excludedPatterns, 1);
    }

    /**
     * 
     */
    public String getLastRequestUrl(final HttpServletRequest request) throws Exception {
        return getRequestUrl(request, new ArrayList<String>(), 1);
    }
    
    /**
     * 
     */
    public String getLastProductDetailsRequestUrl(final HttpServletRequest request) throws Exception {
        return getLastSpecificRequestUrl(request, FoUrls.PRODUCT_DETAILS.getUrlPatternKey());
    }

    /**
     * 
     */
    public String getLastProductBrandDetailsRequestUrl(final HttpServletRequest request) throws Exception {
        return getLastSpecificRequestUrl(request, FoUrls.BRAND_DETAILS.getUrlPatternKey());
    }
    
    /**
     * 
     */
    public String getLastStoreDetailsRequestUrl(final HttpServletRequest request) throws Exception {
        return getLastSpecificRequestUrl(request, FoUrls.STORE_DETAILS.getUrlPatternKey());
    }
    
    /**
     * 
     */
    public String getLastRetailerDetailsRequestUrl(final HttpServletRequest request) throws Exception {
        return getLastSpecificRequestUrl(request, FoUrls.RETAILER_DETAILS.getUrlPatternKey());
    }
    
    /**
     * 
     */
    protected String getLastSpecificRequestUrl(final HttpServletRequest request, String pattern) throws Exception {
        String url = Constants.EMPTY;
        ClickstreamSession clickstreamSession = getClickstreamSession(request);
        final List<ClickstreamRequest> clickstreams = clickstreamSession.getRequests();
        if (clickstreams != null && !clickstreams.isEmpty()) {
            Iterator<ClickstreamRequest> it = clickstreams.iterator();
            while (it.hasNext()) {
                ClickstreamRequest clickstream = (ClickstreamRequest) it.next();
                String uri = clickstream.getRequestURI();
                if (uri.endsWith(".html")) {
                    // TEST IF THE URL MATCH
                    if (uri.contains(pattern)) {
                        url = uri;
                        break;
                    }
                }
            }
        }
        // CLEAN CONTEXT FROM URL
        if (StringUtils.isNotEmpty(url) && !isLocalHostMode(request) && url.contains(request.getContextPath())) {
            url = url.replace(request.getContextPath(), "");
        }
        return handleUrl(url);
    }
    
    /**
     * 
     */
    public String getRequestUrl(final HttpServletRequest request, final List<String> excludedPatterns, int position) throws Exception {
        String url = Constants.EMPTY;
        ClickstreamSession clickstreamSession = getClickstreamSession(request);
        
        final List<ClickstreamRequest> clickstreams = clickstreamSession.getRequests();

        if (clickstreams != null && !clickstreams.isEmpty()) {
            // Clean not html values or exluded patterns
            List<ClickstreamRequest> cleanClickstreams = new ArrayList<ClickstreamRequest>();
            Iterator<ClickstreamRequest> it = clickstreams.iterator();
            while (it.hasNext()) {
                ClickstreamRequest clickstream = (ClickstreamRequest) it.next();
                String uri = clickstream.getRequestURI();
                if (uri.endsWith(".html")) {
                    // TEST IF THE URL IS EXCLUDE
                    CharSequence[] excludedPatternsCharSequence = excludedPatterns.toArray(new CharSequence[excludedPatterns.size()]);
                    boolean isExclude = false;
                    for (int i = 0; i < excludedPatternsCharSequence.length; i++) {
                        CharSequence string = excludedPatternsCharSequence[i];
                        if (uri.contains(string)) {
                            isExclude = true;
                        }
                    }
                    if (BooleanUtils.negate(isExclude)) {
                        cleanClickstreams.add(clickstream);
                    }
                }
            }

            if (cleanClickstreams.size() == 1) {
                Iterator<ClickstreamRequest> itCleanClickstreams = cleanClickstreams.iterator();
                while (itCleanClickstreams.hasNext()) {
                    ClickstreamRequest clickstream = (ClickstreamRequest) itCleanClickstreams.next();
                    String uri = clickstream.getRequestURI();
                    url = uri;
                }
            } else {
                Iterator<ClickstreamRequest> itCleanClickstreams = cleanClickstreams.iterator();
                int countCleanClickstream = 1;
                while (itCleanClickstreams.hasNext()) {
                    ClickstreamRequest clickstream = (ClickstreamRequest) itCleanClickstreams.next();
                    String uri = clickstream.getRequestURI();
                    // The last url is the current URI, so we need to get the url previous the last
                    if (countCleanClickstream == (cleanClickstreams.size() - position)) {
                        url = uri;
                    }
                    countCleanClickstream++;
                }
            }
        }

        // CLEAN CONTEXT FROM URL
        if (StringUtils.isNotEmpty(url) && !isLocalHostMode(request) && url.contains(request.getContextPath())) {
            url = url.replace(request.getContextPath(), "");
        }
        return handleUrl(url);
    }
    
    /**
     * 
     */
    public String getCurrentThemeResourcePrefixPath(final RequestData requestData) throws Exception {
        final HttpServletRequest request = requestData.getRequest();
        EngineSetting engineSetting = engineSettingService.getSettingThemeResourcePrefixPath();
        try {
            String contextValue = getCurrentContextNameValue();
            EngineSettingValue engineSettingValue = engineSetting.getEngineSettingValue(contextValue);
            String prefixPath = engineSetting.getDefaultValue();
            if (engineSettingValue != null) {
                prefixPath = engineSettingValue.getValue();
            } else {
                logger.warn("This engine setting is request, but doesn't exist: " + engineSetting.getCode() + "/" + contextValue);
            }
            String currentThemeResourcePrefixPath = prefixPath + getCurrentTheme(requestData);
            if (currentThemeResourcePrefixPath.endsWith("/")) {
                currentThemeResourcePrefixPath = currentThemeResourcePrefixPath.substring(0, currentThemeResourcePrefixPath.length() - 1);
            }
            return currentThemeResourcePrefixPath;

        } catch (Exception e) {
            logger.error("Context name, " + getContextName() + " can't be resolve by EngineSettingWebAppContext class.", e);
        }
        return null;
    }

    /**
     * 
     */
    public GeolocData getCurrentGeolocData(final HttpServletRequest request) throws Exception {
        final EngineEcoSession engineEcoSession = getCurrentEcoSession(request);
        return engineEcoSession.getGeolocData();
    }

    /**
     * 
     */
    public String getCurrentContextNameValue() throws Exception {
        return PropertiesUtil.getWebappContextKey(getContextName());
    }

    /**
     * 
     */
    public String getCurrentVelocityWebPrefix(final RequestData requestData) throws Exception {
        String velocityPath = "/" + getCurrentTheme(requestData) + "/www/" + getCurrentDevice(requestData) + "/content/";
        return velocityPath;
    }

    /**
     * 
     */
    public String getCurrentVelocityEmailPrefix(final RequestData requestData) throws Exception {
        String velocityPath = "/" + getCurrentTheme(requestData) + "/email/";
        return velocityPath;
    }

    /**
     * 
     */
    protected String handleUrl(String url) {
        return url;
    }

    /**
     * 
     */
    public EngineEcoSession getCurrentEcoSession(final HttpServletRequest request) throws Exception {
        EngineEcoSession engineEcoSession = (EngineEcoSession) request.getSession().getAttribute(Constants.ENGINE_ECO_SESSION_OBJECT);
        return engineEcoSession;
    }

    /**
     * 
     */
    public EngineEcoSession updateCurrentEcoSession(final HttpServletRequest request, EngineEcoSession engineEcoSession) throws Exception {
        setCurrentEcoSession(request, engineEcoSession);
        return engineEcoSession;
    }

    /**
     * 
     */
    public void setCurrentEcoSession(final HttpServletRequest request, final EngineEcoSession engineEcoSession) throws Exception {
        request.getSession().setAttribute(Constants.ENGINE_ECO_SESSION_OBJECT, engineEcoSession);
    }

    
    /**
     * 
     */
    public EngineBoSession getCurrentBoSession(final HttpServletRequest request) throws Exception {
        EngineBoSession engineBoSession = (EngineBoSession) request.getSession().getAttribute(Constants.ENGINE_BO_SESSION_OBJECT);
        return engineBoSession;
    }

    /**
     * 
     */
    public void updateCurrentBoSession(final HttpServletRequest request, final EngineBoSession engineBoSession) throws Exception {
        setCurrentBoSession(request, engineBoSession);
    }

    /**
     * 
     */
    public void setCurrentBoSession(final HttpServletRequest request, final EngineBoSession engineBoSession) throws Exception {
        request.getSession().setAttribute(Constants.ENGINE_BO_SESSION_OBJECT, engineBoSession);
    }

    /**
     * 
     */
    public void resetCurrentCart(final HttpServletRequest request) throws Exception {
        EngineEcoSession engineEcoSession = getCurrentEcoSession(request);
        engineEcoSession.resetCurrentCart();
        updateCurrentEcoSession(request, engineEcoSession);
    }
    
    /**
     * 
     */
    public void updateCurrentCart(final HttpServletRequest request, final Cart cart) throws Exception {
        // SAVE AND UPDATE THE ENGINE SESSION AT THE END
        EngineEcoSession engineEcoSessionWithTransientValues = getCurrentEcoSession(request);
        engineEcoSessionWithTransientValues.updateCart(cart);
        engineEcoSessionWithTransientValues = engineSessionService.updateAndSynchronizeEngineEcoSession(engineEcoSessionWithTransientValues);
        updateCurrentEcoSession(request, engineEcoSessionWithTransientValues); 
    }
    
    /**
     * 
     */
    public void deleteCurrentCartAndSaveEngineSession(final HttpServletRequest request) throws Exception {
        EngineEcoSession engineEcoSessionWithTransientValues = getCurrentEcoSession(request);
        engineEcoSessionWithTransientValues.deleteCurrentCart();
        engineSessionService.updateAndSynchronizeEngineEcoSession(engineEcoSessionWithTransientValues);
        updateCurrentEcoSession(request, engineEcoSessionWithTransientValues); 
    }
    
    /**
     * 
     */
    public OrderCustomer getLastOrder(final RequestData requestData) throws Exception {
        final HttpServletRequest request = requestData.getRequest();
        EngineEcoSession engineEcoSession = getCurrentEcoSession(request);
        return engineEcoSession.getLastOrder();
    }

    /**
     * 
     */
    public void keepLastOrderInSession(final RequestData requestData, final OrderCustomer order) throws Exception {
        if (order != null) {
            final HttpServletRequest request = requestData.getRequest();
            EngineEcoSession engineEcoSession = getCurrentEcoSession(request);
            engineEcoSession.setLastOrder(order);
            updateCurrentEcoSession(request, engineEcoSession);
        }
    }

    /**
     * 
     */
    protected MarketPlace getCurrentMarketPlace(final RequestData requestData) throws Exception {
        MarketPlace marketPlace = null;
        final HttpServletRequest request = requestData.getRequest();
        if (requestData.isBackoffice()) {
            EngineBoSession engineBoSession = getCurrentBoSession(request);
            marketPlace = engineBoSession.getCurrentMarketPlace();
            if (marketPlace == null) {
                initDefaultBoMarketPlace(request);
                marketPlace = engineBoSession.getCurrentMarketPlace();
            }
        } else {
            EngineEcoSession engineEcoSession = getCurrentEcoSession(request);
            marketPlace = engineEcoSession.getCurrentMarketPlace();
            if (marketPlace == null) {
                initEcoMarketPlace(request);
                marketPlace = engineEcoSession.getCurrentMarketPlace();
            }
        }
        return marketPlace;
    }

    /**
     * 
     */
    protected Market getCurrentMarket(final RequestData requestData) throws Exception {
        Market market = null;
        final HttpServletRequest request = requestData.getRequest();
        if (requestData.isBackoffice()) {
            EngineBoSession engineBoSession = getCurrentBoSession(request);
            market = engineBoSession.getCurrentMarket();
            if (market == null) {
                initEcoMarketPlace(request);
                market = engineBoSession.getCurrentMarket();
            }
        } else {
            EngineEcoSession engineEcoSession = getCurrentEcoSession(request);
            market = engineEcoSession.getCurrentMarket();
            if (market == null) {
                initEcoMarketPlace(request);
                market = engineEcoSession.getCurrentMarket();
            }
        }
        return market;
    }

    /**
     * 
     */
    protected MarketArea getCurrentMarketArea(final RequestData requestData) throws Exception {
        MarketArea marketArea = null;
        final HttpServletRequest request = requestData.getRequest();
        if (requestData.isBackoffice()) {
            EngineBoSession engineBoSession = getCurrentBoSession(request);
            marketArea = engineBoSession.getCurrentMarketArea();
            if (marketArea == null) {
                initDefaultBoMarketPlace(request);
                marketArea = engineBoSession.getCurrentMarketArea();
            }
        } else {
            EngineEcoSession engineEcoSession = getCurrentEcoSession(request);
            marketArea = engineEcoSession.getCurrentMarketArea();
            if (marketArea == null) {
                initEcoMarketPlace(request);
                marketArea = engineEcoSession.getCurrentMarketArea();
            }
        }
        return marketArea;
    }

    /**
     * 
     */
    protected Localization getCurrentMarketAreaLocalization(final RequestData requestData) throws Exception {
        Localization localization = null;
        final HttpServletRequest request = requestData.getRequest();
        if (requestData.isBackoffice()) {
            EngineBoSession engineBoSession = getCurrentBoSession(request);
            localization = engineBoSession.getCurrentMarketAreaLocalization();
        } else {
            EngineEcoSession engineEcoSession = getCurrentEcoSession(request);
            localization = engineEcoSession.getCurrentMarketAreaLocalization();
        }
        return localization;
    }

    /**
     * 
     */
    public void updateCurrentLocalization(final RequestData requestData, final Localization localization) throws Exception {
        final HttpServletRequest request = requestData.getRequest();
        if (localization != null) {
            if (requestData.isBackoffice()) {
                EngineBoSession engineBoSession = getCurrentBoSession(request);
                if(engineBoSession != null){
                    engineBoSession.setCurrentBackofficeLocalization(localization);
                    updateCurrentBoSession(request, engineBoSession);
                }
            } else {
                EngineEcoSession engineEcoSession = getCurrentEcoSession(request);
                if(engineEcoSession != null){
                    engineEcoSession = (EngineEcoSession) setSessionMarketAreaLocalization(engineEcoSession, localization);
                    updateCurrentEcoSession(request, engineEcoSession);
                }
            }
        }
    }

    /**
     * 
     */
    protected Retailer getCurrentMarketAreaRetailer(final RequestData requestData) throws Exception {
        Retailer retailer = null;
        final HttpServletRequest request = requestData.getRequest();
        if (requestData.isBackoffice()) {
            EngineBoSession engineBoSession = getCurrentBoSession(request);
            retailer = engineBoSession.getCurrentMarketAreaRetailer();
            if (retailer == null) {
                initDefaultBoMarketPlace(request);
            }
        } else {
            EngineEcoSession engineEcoSession = getCurrentEcoSession(request);
            retailer = engineEcoSession.getCurrentMarketAreaRetailer();
            if (retailer == null) {
                initEcoMarketPlace(request);
            }
        }
        return retailer;
    }

    /**
     * 
     */
    protected CurrencyReferential getCurrentMarketAreaCurrency(final RequestData requestData) throws Exception {
        CurrencyReferential currencyReferential = null;
        final HttpServletRequest request = requestData.getRequest();
        if (requestData.isBackoffice()) {
            EngineBoSession engineBoSession = getCurrentBoSession(request);
            currencyReferential = engineBoSession.getCurrentMarketAreaCurrency();
            if (currencyReferential == null) {
                initDefaultBoMarketPlace(request);
            }
        } else {
            EngineEcoSession engineEcoSession = getCurrentEcoSession(request);
            currencyReferential = engineEcoSession.getCurrentMarketAreaCurrency();
            if (currencyReferential == null) {
                initEcoMarketPlace(request);
            }
        }
        return currencyReferential;
    }

    /**
     * 
     */
    protected Localization getCurrentBackofficeLocalization(final RequestData requestData) throws Exception {
        Localization localization = null;
        final HttpServletRequest request = requestData.getRequest();
        if (requestData.isBackoffice()) {
            EngineBoSession engineBoSession = getCurrentBoSession(request);
            localization = engineBoSession.getCurrentBackofficeLocalization();
        }
        return localization;
    }
    
    /**
     * 
     */
    protected Cart getCurrentCart(final HttpServletRequest request) throws Exception {
        EngineEcoSession engineEcoSession = getCurrentEcoSession(request);
        return engineEcoSession.getCart();
    }
    
    /**
     * 
     */
    protected Customer getCurrentCustomer(final HttpServletRequest request) throws Exception {
        EngineEcoSession engineEcoSession = getCurrentEcoSession(request);
        Customer customer = engineEcoSession.getCurrentCustomer();
        if (customer == null) {
            // CHECK
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                String username = SecurityContextHolder.getContext().getAuthentication().getName();
                if (StringUtils.isNotEmpty(username) && !username.equalsIgnoreCase("anonymousUser")) {
                    customer = customerService.getCustomerByLoginOrEmail(username);
                    updateCurrentCustomer(request, customer);
                }
            }
        }
        return customer;
    }

    /**
     * 
     */
    public String getCustomerAvatar(final HttpServletRequest request, final Customer customer) throws Exception {
        String customerAvatar = null;
        if (customer != null) {
            if (StringUtils.isNotEmpty(customer.getAvatarImg())) {
                customerAvatar = customer.getAvatarImg();
            } else {
                String email = customer.getEmail().toLowerCase().trim();
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] array = md.digest(email.getBytes("CP1252"));
                StringBuffer sb = new StringBuffer();
                for (int i = 0; i < array.length; ++i) {
                    sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1, 3));
                }
                String gravatarId = sb.toString();
                if ("https".equals(request.getScheme())) {
                    customerAvatar = "https://secure.gravatar.com/avatar/" + gravatarId;
                } else {
                    customerAvatar = "http://www.gravatar.com/avatar/" + gravatarId;
                }
            }
        }
        return customerAvatar;
    }

    /**
     * 
     */
    public boolean hasKnownCustomerLogged(final HttpServletRequest request) throws Exception {
        final Customer customer = getCurrentCustomer(request);
        if (customer != null) {
            return true;
        }
        return false;
    }

    /**
     * 
     */
    public Long getCurrentCustomerId(final HttpServletRequest request) throws Exception {
        Customer customer = getCurrentCustomer(request);
        if (customer == null) {
            return null;
        }
        return customer.getId();
    }

    /**
     * 
     */
    public String getCurrentCustomerLogin(final HttpServletRequest request) throws Exception {
        EngineEcoSession session = getCurrentEcoSession(request);
        Customer customer = session.getCurrentCustomer();
        if (customer == null) {
            return null;
        }
        return customer.getLogin();
    }

    /**
     * 
     */
    public void updateCurrentCustomer(final HttpServletRequest request, final Customer customer) throws Exception {
        if (customer != null) {
            final EngineEcoSession session = getCurrentEcoSession(request);
            setSessionCustomer(session, customer);
            updateCurrentEcoSession(request, session);
        }
    }

    /**
     * 
     */
    public void cleanCurrentCustomer(final HttpServletRequest request) throws Exception {
        final EngineEcoSession session = getCurrentEcoSession(request);
        session.setCurrentCustomer(null);
        updateCurrentEcoSession(request, session);
    }



    /**
     * 
     */
    public User getCurrentUser(final HttpServletRequest request) throws Exception {
        EngineBoSession session = getCurrentBoSession(request);
        return session.getCurrentUser();
    }

    /**
     * 
     */
    public void updateCurrentUser(final HttpServletRequest request, final User user) throws Exception {
        if (user != null) {
            final EngineBoSession session = getCurrentBoSession(request);
            if(session != null){
                session.setCurrentUser(user);
            }
            updateCurrentCompany(request, user.getCompany());
            updateCurrentBoSession(request, session);
        }
    }
    
    /**
     * 
     */
    public void cleanCurrentUser(final HttpServletRequest request) throws Exception {
        final EngineBoSession session = getCurrentBoSession(request);
        if(session != null){
            session.setCurrentUser(null);
        }
        updateCurrentBoSession(request, session);
    }

    /**
     * 
     */
    public Company getCurrentCompany(final HttpServletRequest request) throws Exception {
        EngineBoSession session = getCurrentBoSession(request);
        return session.getCurrentCompany();
    }

    public void updateCurrentCompany(final HttpServletRequest request, final Company company) throws Exception {
        if (company != null) {
            final EngineBoSession engineBoSession = getCurrentBoSession(request);
            if(engineBoSession != null){
                Company reloadedCompany = userService.getCompanyById(company.getId().toString());
                engineBoSession.setCurrentCompany(reloadedCompany);
                updateCurrentBoSession(request, engineBoSession);
            }
        }
    }
    
    /**
     * 
     */
    public String getCurrentTheme(final RequestData requestData) throws Exception {
        String currenTheme = "";
        final HttpServletRequest request = requestData.getRequest();
        if (requestData.isBackoffice()) {
            EngineBoSession engineBoSession = getCurrentBoSession(request);
            if(engineBoSession != null){
                currenTheme = engineBoSession.getTheme();
            }
        } else {
            EngineEcoSession engineEcoSession = getCurrentEcoSession(request);
            if(engineEcoSession != null){
                currenTheme = engineEcoSession.getTheme();
            }
        }
        
        // SANITY CHECK
        if (StringUtils.isEmpty(currenTheme)) {
            return "default";
        }
        return currenTheme;
    }

    /**
     * 
     */
    public void updateCurrentTheme(final HttpServletRequest request, final String theme) throws Exception {
        final EngineEcoSession engineEcoSession = getCurrentEcoSession(request);
        if (StringUtils.isNotEmpty(theme)) {
            engineEcoSession.setTheme(theme);
            updateCurrentEcoSession(request, engineEcoSession);
        }
    }

    /**
     * 
     */
    public String getCurrentDevice(final RequestData requestData) throws Exception {
        final HttpServletRequest request = requestData.getRequest();
        String currenDevice = "default";
        if (requestData.isBackoffice()) {
            EngineBoSession engineBoSession = getCurrentBoSession(request);
            if (StringUtils.isNotEmpty(engineBoSession.getDevice())) {
                currenDevice = engineBoSession.getDevice();
            }
        } else {
            EngineEcoSession engineEcoSession = getCurrentEcoSession(request);
            if (StringUtils.isNotEmpty(engineEcoSession.getDevice())) {
                currenDevice = engineEcoSession.getDevice();
            }
        }
        return currenDevice;
    }

    /**
     * 
     */
    public void updateCurrentDevice(final RequestData requestData, final String device) throws Exception {
        final HttpServletRequest request = requestData.getRequest();
        if (requestData.isBackoffice()) {
            final EngineBoSession engineBoSession = getCurrentBoSession(request);
            if (StringUtils.isNotEmpty(device)) {
                engineBoSession.setDevice(device);
                updateCurrentBoSession(request, engineBoSession);
            }
        } else {
            final EngineEcoSession engineEcoSession = getCurrentEcoSession(request);
            if (StringUtils.isNotEmpty(device)) {
                engineEcoSession.setDevice(device);
                updateCurrentEcoSession(request, engineEcoSession);
            }
        }
    }

    /**
     * 
     */
    public RequestData getRequestData(final HttpServletRequest request) throws Exception {
        final RequestData requestData = new RequestData();
        requestData.setRequest(request);
        
        String contextPath = "";
        if (request.getRequestURL().toString().contains("localhost") || request.getRequestURL().toString().contains("127.0.0.1")) {
            contextPath = contextPath + request.getContextPath() + "/";
        } else {
            contextPath = "/";
        }
        requestData.setContextPath(contextPath);
        requestData.setContextNameValue(getCurrentContextNameValue());

        // SPECIFIC BACKOFFICE
        if (requestData.isBackoffice()) {
            checkEngineBoSession(request);
        } else {
            // SPECIFIC FRONTOFFICE
            checkEngineEcoSession(request);
            requestData.setGeolocData(getCurrentGeolocData(request));
        }
        
        requestData.setVelocityEmailPrefix(getCurrentVelocityEmailPrefix(requestData));

        requestData.setMarketPlace(getCurrentMarketPlace(requestData));
        requestData.setMarket(getCurrentMarket(requestData));
        requestData.setMarketArea(getCurrentMarketArea(requestData));
        requestData.setMarketAreaLocalization(getCurrentMarketAreaLocalization(requestData));
        requestData.setMarketAreaRetailer(getCurrentMarketAreaRetailer(requestData));
        requestData.setMarketAreaCurrency(getCurrentMarketAreaCurrency(requestData));
        
        // SPECIFIC BACKOFFICE
        if (requestData.isBackoffice()) {
            User user = getCurrentUser(request);
            if (user != null) {
                requestData.setUser(user);
            }

            Company company = getCurrentCompany(request);
            if (company != null) {
                requestData.setCompany(company);
            }

            requestData.setBackofficeLocalization(getCurrentBackofficeLocalization(requestData));

        } else {
            // SPECIFIC FRONTOFFICE
            Customer customer = getCurrentCustomer(request);
            if (customer != null) {
                requestData.setCustomer(customer);
            }
            
            requestData.setCart(getCurrentCart(request));
        }

        return requestData;
    }

    /**
     * 
     */
    public EngineEcoSession handleGeolocLatitudeLongitude(final RequestData requestData, final String latitude, final String longitude) throws Exception {
        final HttpServletRequest request = requestData.getRequest();
        EngineEcoSession engineEcoSession = getCurrentEcoSession(request);
        if (StringUtils.isNotEmpty(latitude)
                && StringUtils.isNotEmpty(longitude)) {
            // FIND LATITUDE/LONGITUDE BY CITY/COUNTRY
            GeolocData geolocData = requestData.getGeolocData();
            if(geolocData == null){
                geolocData = new GeolocData();
                geolocData.setLatitude(latitude);
                geolocData.setLongitude(longitude);
            }
            
            // TODO : ? requeter pour avoir la ville la plus proche à 5 km
            
            GeolocAddress geolocAddress = geolocService.getGeolocAddressByLatitudeAndLongitude(latitude, longitude);
            if (geolocAddress != null) {
                geolocData.setLatitude(geolocAddress.getLatitude());
                geolocData.setLongitude(geolocAddress.getLongitude());
                
                GeolocDataCountry geolocDataCountry = new GeolocDataCountry();
                geolocDataCountry.setIsoCode(geolocAddress.getCountry());
                geolocDataCountry.setName(referentialDataService.getCountryByLocale(geolocAddress.getCountry(), requestData.getLocale()));
                geolocData.setCountry(geolocDataCountry);
                
                GeolocDataCity geolocDataCity = new GeolocDataCity();
                geolocDataCity.setName(geolocAddress.getCity());
                geolocData.setCity(geolocDataCity);
                
            } else {
                // LATITUDE/LONGITUDE DOESN'T EXIST - WE USE GOOGLE GEOLOC TO FOUND IT
                geolocAddress = geolocService.geolocByLatitudeLongitude(latitude, longitude);
                if (geolocAddress != null) {
                    GeolocDataCountry geolocDataCountry = new GeolocDataCountry();
                    geolocDataCountry.setIsoCode(geolocAddress.getCountry());
                    geolocDataCountry.setName(referentialDataService.getCountryByLocale(geolocAddress.getCountry(), requestData.getLocale()));
                    geolocData.setCountry(geolocDataCountry);
                    
                    GeolocDataCity geolocDataCity = new GeolocDataCity();
                    geolocDataCity.setName(geolocAddress.getCity());
                    geolocData.setCity(geolocDataCity);
                    
                }
            }
            engineEcoSession.setGeolocData(geolocData);
            engineEcoSession = updateCurrentEcoSession(request, engineEcoSession);
        }
        return engineEcoSession;
    }
    
    protected UrlParameterMapping handleUrlParameters(final HttpServletRequest request) {
        UrlParameterMapping urlParameterMapping = new UrlParameterMapping();
        String marketPlaceCode = null;
        String marketCode = null;
        String marketAreaCode = null;
        String localizationCode = null;
        String retailerCode = null;
        String currencyCode = null;
        String requestUri = request.getRequestURI();
        requestUri = requestUri.replace(request.getContextPath(), "");
        if (requestUri.startsWith("/")) {
            requestUri = requestUri.substring(1, requestUri.length());
        }
        String[] uriSegments = requestUri.toString().split("/");
        if (uriSegments.length > 4) {
            marketPlaceCode = uriSegments[0];
            marketCode = uriSegments[1];
            marketAreaCode = uriSegments[2];
            localizationCode = uriSegments[3];
            retailerCode = uriSegments[4];
        } else {
            marketPlaceCode = request.getParameter(RequestConstants.REQUEST_PARAMETER_MARKET_PLACE_CODE);
            marketCode = request.getParameter(RequestConstants.REQUEST_PARAMETER_MARKET_CODE);
            marketAreaCode = request.getParameter(RequestConstants.REQUEST_PARAMETER_MARKET_AREA_CODE);
            localizationCode = request.getParameter(RequestConstants.REQUEST_PARAMETER_MARKET_AREA_LANGUAGE);
            retailerCode = request.getParameter(RequestConstants.REQUEST_PARAMETER_MARKET_AREA_RETAILER_CODE);
            currencyCode = request.getParameter(RequestConstants.REQUEST_PARAMETER_MARKET_AREA_CURRENCY_CODE);
        }
        
        urlParameterMapping.setMarketPlaceCode(marketPlaceCode);
        urlParameterMapping.setMarketCode(marketCode);
        urlParameterMapping.setMarketAreaCode(marketAreaCode);
        urlParameterMapping.setLocalizationCode(localizationCode);
        urlParameterMapping.setRetailerCode(retailerCode);
        urlParameterMapping.setCurrencyCode(currencyCode);
        
        return urlParameterMapping;
    }
    
    /**
	 * 
	 */
    protected EngineEcoSession initEcoSession(final HttpServletRequest request) throws Exception {
        EngineEcoSession engineEcoSession = new EngineEcoSession();
        EngineSetting engineSettingEnvironmentStagingModeEnabled = engineSettingService.getSettingEnvironmentStagingModeEnabled();
        if (engineSettingEnvironmentStagingModeEnabled != null) {
            engineEcoSession.setEnvironmentStagingModeEnabled(BooleanUtils.toBoolean(engineSettingEnvironmentStagingModeEnabled.getDefaultValue()));
        } else {
            engineEcoSession.setEnvironmentStagingModeEnabled(false);
            logger.warn("Environment Type is not define in your database. Check the " + EngineSettingService.ENGINE_SETTING_ENVIRONMENT_STAGING_MODE_ENABLED + " value in settings table.");
        }
        EngineSetting engineSettingEnvironmentType = engineSettingService.getSettingEnvironmentType();
        if (engineSettingEnvironmentType != null) {
            String environmentType = engineSettingEnvironmentType.getDefaultValue();
            try {
                engineEcoSession.setEnvironmentType(EnvironmentType.valueOf(environmentType));
            } catch (Exception e) {
                logger.error("Environment Type has wrong value define in your database. Check the " + EngineSettingService.ENGINE_SETTING_ENVIRONMENT_TYPE + " value in settings table.");
            }
        } else {
            engineEcoSession.setEnvironmentType(EnvironmentType.REEL);
            logger.warn("Environment Type is not define in your database. Check the " + EngineSettingService.ENGINE_SETTING_ENVIRONMENT_TYPE + " value in settings table.");
        }

        // INIT STAGING OR REEL ENVIRONMENT

        setCurrentEcoSession(request, engineEcoSession);
        String jSessionId = request.getSession().getId();
        engineEcoSession.setjSessionId(jSessionId);
        
        // STEP 2 - TRY TO GEOLOC THE CUSTOMER AND SET THE RIGHT MARKET AREA
        engineEcoSession = checkGeolocData(request, engineEcoSession);

        engineEcoSession = initEcoMarketPlace(request);
        
        engineEcoSession = initCart(request);
        
        engineEcoSession = updateCurrentEcoSession(request, engineEcoSession);
        
        engineEcoSession = engineSessionService.saveOrUpdateEngineEcoSession(engineEcoSession);
        
        return engineEcoSession;
    }

    /**
     * 
     */
    protected EngineEcoSession checkGeolocData(final HttpServletRequest request, EngineEcoSession engineEcoSession) throws Exception {
        final String remoteAddress = getRemoteAddr(request);
        GeolocData geolocData = engineEcoSession.getGeolocData();
        if (geolocData == null) {
            geolocData = geolocService.getGeolocData(remoteAddress);
            handleGeolocData(request, engineEcoSession, geolocData);
        } else {
            if (StringUtils.isNotEmpty(geolocData.getRemoteAddress()) 
                    && !geolocData.getRemoteAddress().equals(remoteAddress)) {
                // IP ADDRESS HAS CHANGED - RELOAD
                geolocData = geolocService.getGeolocData(remoteAddress);
                handleGeolocData(request, engineEcoSession, geolocData);
            }
        }
        return engineEcoSession;
    }
    
    protected EngineEcoSession handleGeolocData(final HttpServletRequest request, EngineEcoSession engineEcoSession, final GeolocData geolocData) throws Exception {
        if (geolocData != null) {
            // FIND LATITUDE/LONGITUDE BY CITY/COUNTRY
            GeolocDataCity geolocDataCity = geolocData.getCity();
            GeolocDataCountry geolocDataCountry = geolocData.getCountry();
            if(geolocDataCity != null
                    && geolocDataCountry != null){
                GeolocCity geolocCity = null;
                if(geolocDataCity.getName() != null){
                    geolocCity = geolocService.getGeolocCityByCityAndCountry(geolocDataCity.getName(), geolocDataCountry.getName());
                } else {
                    geolocCity = geolocService.getGeolocCityByCountryWithNullCity(geolocDataCountry.getName());
                }
                if (geolocCity != null) {
                    geolocData.setLatitude(geolocCity.getLatitude());
                    geolocData.setLongitude(geolocCity.getLongitude());
                } else {
                    // LATITUDE/LONGITUDE DOESN'T EXIST - WE USE GOOGLE GEOLOC TO FOUND IT
                    geolocCity = geolocService.geolocByCityAndCountry(geolocDataCity.getName(), geolocDataCountry.getName());
                    geolocData.setLatitude(geolocCity.getLatitude());
                    geolocData.setLongitude(geolocCity.getLongitude());
                }
            }
            engineEcoSession.setGeolocData(geolocData);
            engineEcoSession = updateCurrentEcoSession(request, engineEcoSession);
        }
        return engineEcoSession;
    }
    
    /**
     * 
     */
    protected EngineEcoSession checkEngineEcoSession(final HttpServletRequest request) throws Exception {
        EngineEcoSession engineEcoSession = getCurrentEcoSession(request);
        String jSessionId = request.getSession().getId();
        if (engineEcoSession == null) {
            // RELOAD OLD SESSION
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                Cookie ecoEngineSessionGuid = null;
                for (int i = 0; i < cookies.length; i++) {
                    Cookie cookie = cookies[i];
                    if (getEngineSessionIdCookieName().equals(cookie.getName())) {
                        ecoEngineSessionGuid = cookies[i];
                        break;
                    }
                }
                if(ecoEngineSessionGuid != null){
                    EngineEcoSession engineEcoSessionWithTransientValues = initEcoSession(request);
                    engineSessionService.synchronizeEngineEcoSession(engineEcoSessionWithTransientValues, ecoEngineSessionGuid.getValue());
                }
            }
            if(engineEcoSession == null){
                engineEcoSession = initEcoSession(request);
            }
            
        }
        // SANITY CHECK
        if (!engineEcoSession.getjSessionId().equals(jSessionId)) {
            engineEcoSession.setjSessionId(jSessionId);
            updateCurrentEcoSession(request, engineEcoSession);
        }

        // CHECK GEOLOC DATA : create or reload
        engineEcoSession = checkGeolocData(request, engineEcoSession);

        return engineEcoSession;
    }

    /**
     * 
     */
    protected EngineBoSession checkEngineBoSession(final HttpServletRequest request) throws Exception {
        EngineBoSession engineBoSession = getCurrentBoSession(request);
        if (engineBoSession == null) {
            engineBoSession = initBoSession(request);
        }
        String jSessionId = request.getSession().getId();
        if (!engineBoSession.getjSessionId().equals(jSessionId)) {
            engineBoSession.setjSessionId(jSessionId);
            updateCurrentBoSession(request, engineBoSession);
        }
        return engineBoSession;
    }

    /**
     * 
     */
    protected void initDefaultBoMarketPlace(final HttpServletRequest request) throws Exception {
        EngineBoSession engineBoSession = getCurrentBoSession(request);
        MarketPlace marketPlace = marketService.getDefaultMarketPlace();
        engineBoSession = (EngineBoSession) setSessionMarketPlace(engineBoSession, marketPlace);

        Market market = marketPlace.getDefaultMarket();
        engineBoSession = (EngineBoSession) setSessionMarket(engineBoSession, market);

        MarketArea marketArea = market.getDefaultMarketArea();
        engineBoSession = (EngineBoSession) setSessionMarketArea(engineBoSession, marketArea);

        // DEFAULT LOCALE IS FROM THE REQUEST OR FROM THE MARKET AREA
        String requestLocale = request.getLocale().toString();
        Localization localization = marketArea.getDefaultLocalization();
        if (marketArea.getLocalization(requestLocale) != null) {
            localization = marketArea.getLocalization(requestLocale);
        } else {
            if (requestLocale.length() > 2) {
                String localeLanguage = request.getLocale().getLanguage();
                if (marketArea.getLocalization(localeLanguage) != null) {
                    localization = marketArea.getLocalization(localeLanguage);
                }
            }
        }
        engineBoSession = (EngineBoSession) setSessionMarketAreaLocalization(engineBoSession, localization);

        Retailer retailer = marketArea.getDefaultRetailer();
        engineBoSession = (EngineBoSession) setSessionMarketAreaRetailer(engineBoSession, retailer);

        final CurrencyReferential currency = marketArea.getDefaultCurrency();
        engineBoSession = (EngineBoSession) setSessionMarketAreaCurrency(engineBoSession, currency);

        updateCurrentBoSession(request, engineBoSession);
    }

    /**
	 * 
	 */
    protected EngineBoSession initBoSession(final HttpServletRequest request) throws Exception {
        final EngineBoSession engineBoSession = new EngineBoSession();
        EngineSetting engineSettingEnvironmentStagingModeEnabled = engineSettingService.getSettingEnvironmentStagingModeEnabled();
        if (engineSettingEnvironmentStagingModeEnabled != null) {
            engineBoSession.setEnvironmentStagingModeEnabled(BooleanUtils.toBoolean(engineSettingEnvironmentStagingModeEnabled.getDefaultValue()));
        } else {
            engineBoSession.setEnvironmentStagingModeEnabled(false);
            logger.warn("Environment Type is not define in your database. Check the " + EngineSettingService.ENGINE_SETTING_ENVIRONMENT_STAGING_MODE_ENABLED + " value in settings table.");
        }
        EngineSetting engineSetting = engineSettingService.getSettingEnvironmentType();
        if (engineSetting != null) {
            String environmentType = engineSetting.getDefaultValue();
            try {
                engineBoSession.setEnvironmentType(EnvironmentType.valueOf(environmentType));
            } catch (Exception e) {
                logger.error("Environment Type has wrong value define in your database. Check the " + EngineSettingService.ENGINE_SETTING_ENVIRONMENT_TYPE + " value in settings table.");
            }
        } else {
            engineBoSession.setEnvironmentType(EnvironmentType.REEL);
            logger.warn("Environment Type is not define in your database. Check the " + EngineSettingService.ENGINE_SETTING_ENVIRONMENT_TYPE + " value in settings table.");
        }

        setCurrentBoSession(request, engineBoSession);
        String jSessionId = request.getSession().getId();
        engineBoSession.setjSessionId(jSessionId);
        initDefaultBoMarketPlace(request);

        // Default Localization
        Company company = getCurrentCompany(request);
        if (company != null) {
            // USER IS LOGGED
            engineBoSession.setCurrentBackofficeLocalization(company.getDefaultLocalization());
        } else {
            Localization defaultLocalization = localizationService.getLocalizationByCode("en");
            engineBoSession.setCurrentBackofficeLocalization(defaultLocalization);
        }

        updateCurrentBoSession(request, engineBoSession);
        return engineBoSession;
    }

    /**
     * 
     */
    protected EngineEcoSession initCart(final HttpServletRequest request) throws Exception {
        final EngineEcoSession engineEcoSession = getCurrentEcoSession(request);
        Cart cart = engineEcoSession.getCart();
        if (cart == null) {
            // Init a new empty Cart with a default configuration
            engineEcoSession.addNewCart();
        }
        updateCurrentEcoSession(request, engineEcoSession);
        return engineEcoSession;
    }

    /**
     * @throws Exception
     * 
     */
    protected void resetCart(final HttpServletRequest request) throws Exception {
        // Reset Cart
        final EngineEcoSession engineEcoSession = getCurrentEcoSession(request);
        engineEcoSession.resetCurrentCart();
        updateCurrentEcoSession(request, engineEcoSession);
    }

    /**
     * 
     */
    protected MarketArea evaluateMarketPlace(final HttpServletRequest request) throws Exception {
        EngineEcoSession engineEcoSession = getCurrentEcoSession(request);
        MarketPlace marketPlace = null;
        Market market = null;
        MarketArea marketArea = null;
        
        if(engineEcoSession == null){
            initEcoSession(request);
        }
        
        // STEP 1 - CHECK THE URL PARAMETERS
        UrlParameterMapping urlParameterMapping = handleUrlParameters(request);
        String marketPlaceCode = urlParameterMapping.getMarketPlaceCode();
        if(StringUtils.isNotEmpty(marketPlaceCode)){
            marketPlace = marketService.getMarketPlaceByCode(marketPlaceCode);
            if(marketPlace != null){
                String marketCode = urlParameterMapping.getMarketCode();
                market = marketPlace.getMarket(marketCode);
                if(market != null){
                    String marketAreaCode = urlParameterMapping.getMarketAreaCode();
                    marketArea = market.getMarketArea(marketAreaCode);
                    return marketArea;
                }
            }
        }

        // STEP 2 - TRY TO GEOLOC THE CUSTOMER AND SET THE RIGHT MARKET AREA
        final GeolocData geolocData = engineEcoSession.getGeolocData();
        MarketArea marketAreaGeoloc = null;
        if(geolocData != null){
            final GeolocDataCountry country = geolocData.getCountry();
            if(country != null && StringUtils.isNotEmpty(country.getIsoCode())){
                List<MarketArea> marketAreas = marketService.getMarketAreaByGeolocCountryCode(country.getIsoCode());
                if(marketAreas != null && marketAreas.size() == 1){
                    marketAreaGeoloc = marketAreas.get(0);
                } else {
                    // WE HAVE MANY MARKET AREA FOR THE CURRENT COUNTRY CODE - WE SELECT THE DEFAULT MARKET PLACE ASSOCIATE
                    for (Iterator<MarketArea> iterator = marketAreas.iterator(); iterator.hasNext();) {
                        MarketArea marketAreaIt = (MarketArea) iterator.next();
                        if(marketAreaIt.getMarket().getMarketPlace().isDefault()){
                            marketAreaGeoloc = marketAreaIt;
                        }
                    }
                }
            }
        }
        
        if (marketAreaGeoloc != null) {
            marketPlace = marketService.getMarketPlaceByCode(marketAreaGeoloc.getMarket().getMarketPlace().getCode());
            market = marketAreaGeoloc.getMarket();
            marketArea = marketAreaGeoloc;
            return marketArea;
        }

        // STEP 3 - DEFAULT MARTKETPLACE
        marketPlace = marketService.getDefaultMarketPlace();
        market = marketPlace.getDefaultMarket();
        marketArea = market.getDefaultMarketArea();
        
        return marketArea;
    }
    
    /**
     * 
     */
    protected EngineEcoSession initEcoMarketPlace(final HttpServletRequest request) throws Exception {
        EngineEcoSession engineEcoSession = getCurrentEcoSession(request);
        MarketArea marketArea = evaluateMarketPlace(request);
        Market market = marketArea.getMarket();
        MarketPlace marketPlace = market.getMarketPlace();
        
        engineEcoSession = (EngineEcoSession) setSessionMarketPlace(engineEcoSession, marketPlace);
        engineEcoSession = (EngineEcoSession) setSessionMarket(engineEcoSession, market);
        engineEcoSession = (EngineEcoSession) setSessionMarketArea(engineEcoSession, marketArea);

        // DEFAULT LOCALE IS FROM THE REQUEST OR FROM THE MARKET AREA
        marketArea = engineEcoSession.getCurrentMarketArea();
        final String requestLocale = request.getLocale().toString();
        Localization localization = marketArea.getDefaultLocalization();
        if (marketArea.getLocalization(requestLocale) != null) {
            localization = marketArea.getLocalization(requestLocale);
        } else {
            if (requestLocale.length() > 2) {
                String localeLanguage = request.getLocale().getLanguage();
                if (marketArea.getLocalization(localeLanguage) != null) {
                    localization = marketArea.getLocalization(localeLanguage);
                }
            }
        }
        engineEcoSession = (EngineEcoSession) setSessionMarketAreaLocalization(engineEcoSession, localization);

        final Retailer retailer = marketArea.getDefaultRetailer();
        engineEcoSession = (EngineEcoSession) setSessionMarketAreaRetailer(engineEcoSession, retailer);

        final CurrencyReferential currency = marketArea.getDefaultCurrency();
        engineEcoSession = (EngineEcoSession) setSessionMarketAreaCurrency(engineEcoSession, currency);

        setCurrentEcoSession(request, engineEcoSession);
        
        return engineEcoSession; 
    }
    
    protected AbstractEngineSession setSessionMarketPlace(final AbstractEngineSession session, final MarketPlace marketPlace){
        session.setCurrentMarketPlace(marketService.getMarketPlaceById(marketPlace.getId().toString()));
        return session;
    }

    protected AbstractEngineSession setSessionMarket(final AbstractEngineSession session, final Market market){
        session.setCurrentMarket(marketService.getMarketById(market.getId().toString()));
        return session;
    }

    protected AbstractEngineSession setSessionMarketArea(final AbstractEngineSession session, final MarketArea marketArea){
        session.setCurrentMarketArea(marketService.getMarketAreaById(marketArea.getId().toString()));
        return session;
    }

    protected AbstractEngineSession setSessionMarketAreaLocalization(final AbstractEngineSession session, final Localization localization){
        session.setCurrentMarketAreaLocalization(localizationService.getLocalizationById(localization.getId().toString()));
        return session;
    }

    protected AbstractEngineSession setSessionMarketAreaRetailer(final AbstractEngineSession session, final Retailer retailer){
        session.setCurrentMarketAreaRetailer(retailerService.getRetailerById(retailer.getId().toString()));
        return session;
    }
    
    protected AbstractEngineSession setSessionMarketAreaCurrency(final AbstractEngineSession session, final CurrencyReferential currency){
        session.setCurrentMarketAreaCurrency(currencyReferentialService.getCurrencyReferentialById(currency.getId().toString()));
        return session;
    }
    
    protected EngineEcoSession setSessionCustomer(final EngineEcoSession session, final Customer customer){
        session.setCurrentCustomer(customerService.getCustomerById(customer.getId().toString(), FetchPlanGraphCustomer.fullCustomerFetchPlan()));
        return session;
    }
    
    /**
     * @throws Exception 
     * 
     */
    public String getAppName(HttpServletRequest request) throws Exception {
        final RequestData requestData = getRequestData(request);
        final Locale locale = requestData.getLocale();
        Object[] params = {StringUtils.capitalize(getApplicationName())};
        String appName = coreMessageSource.getCommonMessage(ScopeCommonMessage.APP.getPropertyKey(), "name_text", params, locale);
        return appName;
    }

    /**
     * {@inheritDoc}
     */
    public List<String> getRecentProductCodesFromCookie(final HttpServletRequest request, final String catalogVirtualCode){
		Cookie info = null;
        Cookie[] cookies = request.getCookies();
        Boolean found = false;
        if(cookies !=  null){
	        for(int i = 0; i < cookies.length; i++) {
	            info = cookies[i];
	            if(getRecentProductsCookieName().equals(info.getName())) {
	                found = true;
	                break;
	            }
	        }
        }   
        List<String> cookieProductValues = new ArrayList<String>();
        if(found){
	        try {
	        	String value = URLDecoder.decode(info.getValue(), Constants.UTF8);
	        	if(StringUtils.isNotEmpty(value)
	        	        && value.contains(catalogVirtualCode)){
	        	    if(value.contains(Constants.PIPE)){
                        String[] splits = value.split(Constants.PIPE);
                        for (int i = 0; i < splits.length; i++) {
                            String splitValue = splits[i];
                            if(splitValue.contains(catalogVirtualCode)){
                                cookieProductValues.add(splits[i]);
                            }
                        }
	        	    } else {
	        	    	cookieProductValues.add(value);
	        	    }
	        	}
            } catch (UnsupportedEncodingException e) {
                logger.error("Cookie decode value", e);
            }
        } 
        return cookieProductValues;
    }
    
    public void addOrUpdateRecentProductToCookie(final HttpServletRequest request, final HttpServletResponse response,
                                                    final String catalogCode, final String virtualCategoryCode, 
                                                    final String productMarketingCode, final String productSkuCode) throws Exception {
        Cookie info = null;
        String cookieProductValue = catalogCode + Constants.SEMI_COLON + virtualCategoryCode + Constants.SEMI_COLON + productMarketingCode + Constants.SEMI_COLON + productSkuCode;
        Cookie[] cookies = request.getCookies();
        Boolean found = false;
        String domain = request.getServerName();
        if(cookies !=  null){
	        for(int i=0; i < cookies.length; i++) {
	            info = cookies[i];
	            if(getRecentProductsCookieName().equals(info.getName())) {
	                found = true;
	                break;
	            }
	        }
        }   
        if(found){
        	Boolean flag = false;
        	String value = URLDecoder.decode(info.getValue(), Constants.UTF8);
        	if(value.contains(Constants.PIPE)){
                String[] splits = value.split(Constants.PIPE);
                for(String cookieProductValueIt : splits){
                    if(cookieProductValueIt.contains(Constants.SEMI_COLON)){
                        if(cookieProductValueIt.contains(cookieProductValue)){
                            flag = true;
                        } 
                    } else {
                        // VALUE DOESN'T CONTAIN SEMI COLON : CLEAN THE COOKIE - NON COMPATIBLE VALUE
                        info.setValue("");
                        info.setPath("/");
                        info.setMaxAge(Constants.COOKIES_LENGTH);
                        info.setDomain(domain);
                        response.addCookie(info);               
                    }
                }
        	} else {
        		if(value.contains(Constants.SEMI_COLON)){
                    if(value.contains(cookieProductValue)){
                        flag = true;
                    } 
                } else {
                    // VALUE DOESN'T CONTAIN SEMI COLON : CLEAN THE COOKIE - NON COMPATIBLE VALUE
                	value = "";
                    info.setValue("");
                    info.setPath("/");
                    info.setMaxAge(Constants.COOKIES_LENGTH);
                    info.setDomain(domain);
                    response.addCookie(info);               
                }
        	}
            if(!flag){
                String values = value;
                if(StringUtils.isNotEmpty(values)){
                    values += Constants.PIPE;
                }
                values += cookieProductValue;
                info.setValue(URLEncoder.encode(values, Constants.UTF8));
                info.setPath("/");
                info.setMaxAge(Constants.COOKIES_LENGTH);
                info.setDomain(domain);
                response.addCookie(info);               
            } 
        } else {
			info = new Cookie(getRecentProductsCookieName(), cookieProductValue);
            info.setPath("/");
			info.setMaxAge(Constants.COOKIES_LENGTH);
			info.setDomain(domain);
			response.addCookie(info);
        }
    }
    
    /**
     * @throws Exception 
     * 
     */
    public String decodeRecentProductCookieVirtualCatalogCode(String cookieProductValue) throws Exception {
        String[] cookieProductValueSplit = cookieProductValue.split(Constants.SEMI_COLON);
        return cookieProductValueSplit[0];
    }
    
    /**
     * @throws Exception 
     * 
     */
    public String decodeRecentProductCookieVirtualCategoryCode(String cookieProductValue) throws Exception {
        String[] cookieProductValueSplit = cookieProductValue.split(Constants.SEMI_COLON);
        return cookieProductValueSplit[1];
    }
    
    /**
     * @throws Exception 
     * 
     */
    public String decodeRecentProductCookieProductMarketingCode(String cookieProductValue) throws Exception {
        String[] cookieProductValueSplit = cookieProductValue.split(Constants.SEMI_COLON);
        return cookieProductValueSplit[2];
    }
    
    /**
     * @throws Exception 
     * 
     */
    public String decodeRecentProductCookieProductSkuCode(String cookieProductValue) throws Exception {
        String[] cookieProductValueSplit = cookieProductValue.split(Constants.SEMI_COLON);
        return cookieProductValueSplit[3];
    }
    
    protected String getEngineSessionIdCookieName(){
        return cookiePrefix + Constants.COOKIE_ECO_ENGINE_SESSION_ID;
    }
    
    protected String getRecentProductsCookieName(){
        return cookiePrefix + Constants.COOKIE_RECENT_PRODUCT_COOKIE_NAME;
    }
    
}