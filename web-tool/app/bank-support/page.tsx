'use client'

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { CheckCircle2, XCircle, AlertCircle } from 'lucide-react'
import Link from 'next/link'
import { Button } from '@/components/ui/button'

export default function BankSupport() {
  const bankSupport = [
    { name: 'HDFC Bank', credit: true, debit: true, upiSend: true, upiReceive: true, atm: true, card: true, balance: 'default', reference: true },
    { name: 'SBI', credit: true, debit: true, upiSend: true, upiReceive: true, atm: true, card: false, balance: 'custom', reference: true },
    { name: 'ICICI Bank', credit: true, debit: true, upiSend: true, upiReceive: true, atm: true, card: true, balance: 'default', reference: true },
    { name: 'Axis Bank', credit: true, debit: true, upiSend: true, upiReceive: true, atm: true, card: false, balance: 'default', reference: true },
    { name: 'Indian Bank', credit: true, debit: true, upiSend: true, upiReceive: true, atm: true, card: false, balance: 'custom', reference: true },
    { name: 'Federal Bank', credit: true, debit: true, upiSend: true, upiReceive: true, atm: false, card: false, balance: 'default', reference: false },
    { name: 'PNB', credit: true, debit: true, upiSend: false, upiReceive: false, atm: true, card: false, balance: 'custom', reference: false },
    { name: 'IDBI Bank', credit: true, debit: true, upiSend: true, upiReceive: true, atm: true, card: false, balance: 'custom', reference: true },
    { name: 'Karnataka Bank', credit: true, debit: true, upiSend: true, upiReceive: true, atm: true, card: false, balance: 'custom', reference: true },
    { name: 'Canara Bank', credit: true, debit: true, upiSend: true, upiReceive: true, atm: true, card: false, balance: 'custom', reference: true },
    { name: 'Bank of Baroda', credit: true, debit: true, upiSend: true, upiReceive: true, atm: true, card: false, balance: 'custom', reference: true },
    { name: 'Jio Payments', credit: true, debit: true, upiSend: true, upiReceive: true, atm: false, card: false, balance: 'custom', reference: true },
    { name: 'Jupiter', credit: true, debit: true, upiSend: true, upiReceive: true, atm: false, card: false, balance: 'default', reference: true },
    { name: 'Amazon Pay', credit: true, debit: true, upiSend: true, upiReceive: true, atm: false, card: false, balance: 'default', reference: true },
    { name: 'IDFC First', credit: true, debit: true, upiSend: true, upiReceive: true, atm: false, card: false, balance: 'custom', reference: true },
  ]

  const smsTemplates = {
    'HDFC': [
      { type: 'Credit', pattern: 'Rs.XXX credited to a/c **XXXX', example: 'Rs.5000 credited to a/c **3456 on 15-01' },
      { type: 'Debit', pattern: 'Rs.XXX debited from a/c **XXXX', example: 'Rs.2000 debited from a/c **3456 on 15-01' },
      { type: 'UPI', pattern: 'Rs.XXX debited for UPI/', example: 'Rs.500 debited for UPI/PhonePe/merchant@ybl' },
      { type: 'ATM', pattern: 'debited from a/c at ATM', example: 'Rs.3000 debited from a/c **3456 at ATM' },
    ],
    'SBI': [
      { type: 'Credit', pattern: 'Rs.XXX credited to A/c', example: 'Rs.5000 credited to A/c XX1234' },
      { type: 'Credit Alt', pattern: 'A/c XXXX-credited by Rs.XXX', example: 'A/c X9338-credited by Rs.500' },
      { type: 'Debit', pattern: 'Rs.XXX debited from A/c', example: 'Rs.2000 debited from A/c XX1234' },
      { type: 'Debit Alt', pattern: 'A/C XXXX debited by XXX', example: 'A/C X9474 debited by 370.0' },
      { type: 'UPI Send', pattern: 'trf to MERCHANT', example: 'debited by 100 trf to SHOPKEEPER' },
      { type: 'ATM', pattern: 'ATM withdrawal of Rs.XXX', example: 'ATM withdrawal of Rs.2000' },
    ],
    'ICICI': [
      { type: 'Credit', pattern: 'INR XXX credited to A/c', example: 'INR 5000 credited to A/c XX456' },
      { type: 'Debit', pattern: 'Acct XXXX debited with INR', example: 'Acct XX456 debited with INR 2000' },
      { type: 'UPI', pattern: 'linked to VPA', example: 'Rs 500 debited from A/c linked to VPA merchant@icici' },
    ],
    'Indian Bank': [
      { type: 'Credit', pattern: 'Rs.XXX credited to a/c', example: 'Rs.589.00 credited to a/c *3829' },
      { type: 'Credit Alt', pattern: 'credited Rs. XXX', example: 'Your a/c credited Rs. 5000.00' },
      { type: 'Debit', pattern: 'debited Rs. XXX', example: 'Your a/c debited Rs. 2000.00' },
      { type: 'UPI', pattern: 'linked to VPA XXX@XXX', example: 'by a/c linked to VPA 7970282159-2@axl' },
    ],
  }

  const balancePatterns = [
    { bank: 'SBI', patterns: ['Avl Bal Rs XXX', 'Available Balance: Rs XXX', 'Bal: Rs XXX'] },
    { bank: 'Indian Bank', patterns: ['Bal Rs. XXX', 'Available Balance: Rs. XXX'] },
    { bank: 'PNB', patterns: ['Bal INR XXX', 'Bal Rs.XXX'] },
    { bank: 'IDBI', patterns: ['Bal Rs XXX'] },
    { bank: 'IDFC First', patterns: ['New Bal :INR XXX'] },
    { bank: 'Karnataka Bank', patterns: ['Balance is Rs.XXX'] },
    { bank: 'Canara Bank', patterns: ['Total Avail.bal INR XXX'] },
    { bank: 'Bank of Baroda', patterns: ['AvlBal:RsXXX'] },
    { bank: 'Jio Payments', patterns: ['Avl. Bal: Rs. XXX'] },
  ]

  const senderPatterns = [
    { bank: 'HDFC', patterns: ['HDFC', 'HDFCBK', 'XX-HDFC-S', 'XX-HDFCBK-S'] },
    { bank: 'SBI', patterns: ['SBI', 'SBIBK', 'SBIUPI', 'XX-SBIBK-S', 'XX-SBI-S'] },
    { bank: 'ICICI', patterns: ['ICICI', 'ICICIB', 'XX-ICICI-S', 'XX-ICICIB-S'] },
    { bank: 'Axis', patterns: ['AXIS', 'AXISBK', 'XX-AXIS-S', 'XX-AXISBK-S'] },
    { bank: 'Indian Bank', patterns: ['INDBNK', 'INDIAN', 'XX-INDBNK-S'] },
    { bank: 'Federal', patterns: ['FEDERA', 'FEDERALBNK', 'XX-FEDERA-S'] },
    { bank: 'PNB', patterns: ['PNB', 'PNBSMS', 'XX-PNB-S'] },
  ]

  const getIcon = (supported: boolean | string) => {
    if (supported === true) return <CheckCircle2 className="h-5 w-5 text-green-500" />
    if (supported === 'default') return <AlertCircle className="h-5 w-5 text-yellow-500" />
    if (supported === 'custom') return <CheckCircle2 className="h-5 w-5 text-green-500" />
    return <XCircle className="h-5 w-5 text-gray-400" />
  }

  return (
    <div className="container mx-auto py-8 px-4 max-w-6xl">
      <div className="space-y-6">
        <div className="flex justify-between items-start">
          <div>
            <h1 className="text-3xl font-bold">Bank Support Documentation</h1>
            <p className="text-muted-foreground mt-2">
              Comprehensive overview of supported banks and SMS patterns
            </p>
          </div>
          <Link href="/">
            <Button variant="outline">Back to Parser</Button>
          </Link>
        </div>

        <Tabs defaultValue="overview" className="space-y-4">
          <TabsList className="grid w-full grid-cols-4">
            <TabsTrigger value="overview">Overview</TabsTrigger>
            <TabsTrigger value="templates">SMS Templates</TabsTrigger>
            <TabsTrigger value="balance">Balance Patterns</TabsTrigger>
            <TabsTrigger value="senders">Sender IDs</TabsTrigger>
          </TabsList>

          <TabsContent value="overview" className="space-y-4">
            <Card>
              <CardHeader>
                <CardTitle>Supported Banks & Features</CardTitle>
                <CardDescription>
                  {bankSupport.length} banks with various levels of support
                </CardDescription>
              </CardHeader>
              <CardContent>
                <div className="overflow-x-auto -mx-6 px-6 lg:mx-0 lg:px-0">
                  <table className="w-full text-sm min-w-[640px]">
                    <thead>
                      <tr className="border-b">
                        <th className="text-left py-2 px-2 sticky left-0 bg-background z-10 shadow-[2px_0_5px_-2px_rgba(0,0,0,0.1)]">Bank</th>
                        <th className="text-center py-2 px-2">Credit</th>
                        <th className="text-center py-2 px-2">Debit</th>
                        <th className="text-center py-2 px-2 hidden sm:table-cell">UPI Send</th>
                        <th className="text-center py-2 px-2 hidden sm:table-cell">UPI Receive</th>
                        <th className="text-center py-2 px-2">ATM</th>
                        <th className="text-center py-2 px-2 hidden md:table-cell">Card</th>
                        <th className="text-center py-2 px-2">Balance</th>
                        <th className="text-center py-2 px-2 hidden lg:table-cell">Reference</th>
                      </tr>
                    </thead>
                    <tbody>
                      {bankSupport.map((bank) => (
                        <tr key={bank.name} className="border-b">
                          <td className="py-2 px-2 font-medium sticky left-0 bg-background z-10 shadow-[2px_0_5px_-2px_rgba(0,0,0,0.1)]">{bank.name}</td>
                          <td className="py-2 px-2">
                            <div className="flex justify-center">
                              {getIcon(bank.credit)}
                            </div>
                          </td>
                          <td className="py-2 px-2">
                            <div className="flex justify-center">
                              {getIcon(bank.debit)}
                            </div>
                          </td>
                          <td className="py-2 px-2 hidden sm:table-cell">
                            <div className="flex justify-center">
                              {getIcon(bank.upiSend)}
                            </div>
                          </td>
                          <td className="py-2 px-2 hidden sm:table-cell">
                            <div className="flex justify-center">
                              {getIcon(bank.upiReceive)}
                            </div>
                          </td>
                          <td className="py-2 px-2">
                            <div className="flex justify-center">
                              {getIcon(bank.atm)}
                            </div>
                          </td>
                          <td className="py-2 px-2 hidden md:table-cell">
                            <div className="flex justify-center">
                              {getIcon(bank.card)}
                            </div>
                          </td>
                          <td className="py-2 px-2">
                            <div className="flex justify-center">
                              {bank.balance === 'custom' ? (
                                <Badge variant="default" className="text-xs">Custom</Badge>
                              ) : (
                                <Badge variant="secondary" className="text-xs">Default</Badge>
                              )}
                            </div>
                          </td>
                          <td className="py-2 px-2 hidden lg:table-cell">
                            <div className="flex justify-center">
                              {getIcon(bank.reference)}
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
                <div className="mt-4 space-y-2">
                  <div className="flex flex-wrap gap-4 text-sm text-muted-foreground">
                    <div className="flex items-center gap-1">
                      <CheckCircle2 className="h-4 w-4 text-green-500" />
                      <span>Supported</span>
                    </div>
                    <div className="flex items-center gap-1">
                      <AlertCircle className="h-4 w-4 text-yellow-500" />
                      <span>Limited Support</span>
                    </div>
                    <div className="flex items-center gap-1">
                      <XCircle className="h-4 w-4 text-gray-400" />
                      <span>Not Supported</span>
                    </div>
                  </div>
                  <p className="text-xs text-muted-foreground lg:hidden">
                    💡 Swipe horizontally to see all columns. Some columns are hidden on smaller screens.
                  </p>
                </div>
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="templates" className="space-y-4">
            {Object.entries(smsTemplates).map(([bank, templates]) => (
              <Card key={bank}>
                <CardHeader>
                  <CardTitle>{bank} Bank SMS Templates</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="space-y-3">
                    {templates.map((template, idx) => (
                      <div key={idx} className="border rounded-lg p-3">
                        <div className="flex items-center gap-2 mb-2">
                          <Badge variant="outline">{template.type}</Badge>
                        </div>
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-2 text-sm">
                          <div>
                            <span className="font-medium">Pattern: </span>
                            <code className="bg-muted px-1 rounded">{template.pattern}</code>
                          </div>
                          <div>
                            <span className="font-medium">Example: </span>
                            <span className="text-muted-foreground">{template.example}</span>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                </CardContent>
              </Card>
            ))}
          </TabsContent>

          <TabsContent value="balance" className="space-y-4">
            <Card>
              <CardHeader>
                <CardTitle>Balance Extraction Patterns</CardTitle>
                <CardDescription>
                  Banks with custom balance patterns vs default patterns
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <div>
                  <h3 className="font-semibold mb-3">Banks with Custom Patterns</h3>
                  <div className="space-y-2">
                    {balancePatterns.map((bank) => (
                      <div key={bank.bank} className="border rounded-lg p-3">
                        <div className="font-medium mb-2">{bank.bank}</div>
                        <div className="flex flex-wrap gap-2">
                          {bank.patterns.map((pattern, idx) => (
                            <code key={idx} className="bg-muted px-2 py-1 rounded text-sm">
                              {pattern}
                            </code>
                          ))}
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
                
                <div>
                  <h3 className="font-semibold mb-3">Banks Using Default Patterns</h3>
                  <div className="bg-muted rounded-lg p-4">
                    <p className="text-sm mb-2">These banks use the default balance patterns:</p>
                    <div className="flex flex-wrap gap-2">
                      <code className="bg-background px-2 py-1 rounded text-sm">Bal:</code>
                      <code className="bg-background px-2 py-1 rounded text-sm">Balance:</code>
                      <code className="bg-background px-2 py-1 rounded text-sm">Avl Bal</code>
                      <code className="bg-background px-2 py-1 rounded text-sm">Available Balance</code>
                    </div>
                    <div className="flex flex-wrap gap-2 mt-3">
                      {['HDFC Bank', 'ICICI Bank', 'Axis Bank', 'Federal Bank', 'Jupiter', 'Amazon Pay'].map((bank) => (
                        <Badge key={bank} variant="secondary">{bank}</Badge>
                      ))}
                    </div>
                  </div>
                </div>
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="senders" className="space-y-4">
            <Card>
              <CardHeader>
                <CardTitle>Sender ID Patterns</CardTitle>
                <CardDescription>
                  Common sender IDs used by each bank
                </CardDescription>
              </CardHeader>
              <CardContent>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {senderPatterns.map((bank) => (
                    <div key={bank.bank} className="border rounded-lg p-3">
                      <div className="font-medium mb-2">{bank.bank}</div>
                      <div className="flex flex-wrap gap-1">
                        {bank.patterns.map((pattern, idx) => (
                          <Badge key={idx} variant="outline" className="text-xs">
                            {pattern}
                          </Badge>
                        ))}
                      </div>
                    </div>
                  ))}
                </div>
                <div className="mt-4 p-3 bg-muted rounded-lg">
                  <p className="text-sm text-muted-foreground">
                    <strong>Note:</strong> XX in patterns represents state codes (e.g., AD-INDBNK-S, BV-HDFC-S)
                  </p>
                </div>
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>

        <Card>
          <CardHeader>
            <CardTitle>Need Support for Your Bank?</CardTitle>
            <CardDescription>
              If your bank isn't listed or patterns aren't working
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <p className="text-sm">
              You can help us add support for your bank by testing your SMS messages and submitting 
              the correct parsing details.
            </p>
            <div className="flex gap-2">
              <Link href="/">
                <Button>Test Your SMS</Button>
              </Link>
              <Link href="https://github.com/sarim2000/pennywiseai-tracker/issues" target="_blank">
                <Button variant="outline">Report Issue on GitHub</Button>
              </Link>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}