package com.pennywiseai.tracker.data.parser.bank

/**
 * Factory for creating bank-specific parsers based on SMS sender.
 */
object BankParserFactory {
    
    private val parsers = listOf(
        HDFCBankParser(),
        SBIBankParser(),
        IndianBankParser(),
        FederalBankParser(),
        JuspayParser(),
        SliceParser(),
        UtkarshBankParser(),
        ICICIBankParser(),
        KarnatakaBankParser(),
        IDBIBankParser(),
        JupiterBankParser(),
        AxisBankParser(),
        PNBBankParser(),
        CanaraBankParser(),
        BankOfBarodaParser(),
        JioPaymentsBankParser(),
        KotakBankParser(),
        IDFCFirstBankParser(),
        UnionBankParser(),
        HSBCBankParser(),
        CentralBankOfIndiaParser()
        // Add more bank parsers here as we implement them
        // IndusIndBankParser(),
        // etc.
    )
    
    /**
     * Returns the appropriate bank parser for the given sender.
     * Returns null if no specific parser is found.
     */
    fun getParser(sender: String): BankParser? {
        return parsers.firstOrNull { it.canHandle(sender) }
    }
    
    /**
     * Returns all available bank parsers.
     */
    fun getAllParsers(): List<BankParser> = parsers
    
    /**
     * Checks if the sender belongs to any known bank.
     */
    fun isKnownBankSender(sender: String): Boolean {
        return parsers.any { it.canHandle(sender) }
    }
}