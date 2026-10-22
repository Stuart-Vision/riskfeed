package io.github.stuartvision.riskfeed.batch;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/** JAXB mapping of one {@code <position>} element in an XML position file. */
@XmlRootElement(name = "position")
@XmlAccessorType(XmlAccessType.FIELD)
public class PositionXml {

    @XmlElement String tradeId;
    @XmlElement String book;
    @XmlElement String counterpartyLei;
    @XmlElement String product;
    @XmlElement String currency;
    @XmlElement String notional;
    @XmlElement String marketValue;
    @XmlElement String asOfDate;
}
