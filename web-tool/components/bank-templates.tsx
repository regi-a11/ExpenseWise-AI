'use client'

import { useState } from 'react'
import { bankTemplates } from '@/lib/parsers/templates'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Button } from '@/components/ui/button'
import { ChevronDown, ChevronUp } from 'lucide-react'
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from '@/components/ui/collapsible'

export function BankTemplates() {
  const [expandedBanks, setExpandedBanks] = useState<string[]>([])

  const toggleBank = (bankCode: string) => {
    setExpandedBanks(prev => 
      prev.includes(bankCode) 
        ? prev.filter(b => b !== bankCode)
        : [...prev, bankCode]
    )
  }

  const getCategoryColor = (category?: string) => {
    switch(category) {
      case 'debit': return 'destructive'
      case 'credit': return 'default'
      case 'upi': return 'secondary'
      case 'salary': return 'default'
      case 'card': return 'outline'
      case 'atm': return 'outline'
      case 'mandate': return 'secondary'
      default: return 'secondary'
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Supported Message Templates</CardTitle>
        <CardDescription>
          See what types of SMS messages each bank parser can handle
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        {Object.entries(bankTemplates).map(([bankCode, bank]) => (
          <Collapsible
            key={bankCode}
            open={expandedBanks.includes(bankCode)}
            onOpenChange={() => toggleBank(bankCode)}
          >
            <div className="border rounded-lg">
              <CollapsibleTrigger asChild>
                <Button
                  variant="ghost"
                  className="w-full justify-between p-4 h-auto"
                >
                  <div className="flex items-center gap-3">
                    <span className="font-medium">{bank.bankName}</span>
                    <Badge variant={
                      bank.supportLevel === 'full' ? 'default' : 
                      bank.supportLevel === 'partial' ? 'secondary' : 
                      'outline'
                    }>
                      {bank.supportLevel}
                    </Badge>
                    <span className="text-sm text-muted-foreground">
                      {bank.templates.length} templates
                    </span>
                  </div>
                  {expandedBanks.includes(bankCode) ? (
                    <ChevronUp className="h-4 w-4" />
                  ) : (
                    <ChevronDown className="h-4 w-4" />
                  )}
                </Button>
              </CollapsibleTrigger>
              
              <CollapsibleContent className="p-4 pt-0">
                <div className="space-y-3">
                  {bank.templates.map((template, idx) => (
                    <div key={idx} className="border-l-2 pl-4 space-y-2">
                      <div className="flex items-center gap-2">
                        <span className="font-medium text-sm">{template.name}</span>
                        {template.category && (
                          <Badge variant={getCategoryColor(template.category) as any} className="text-xs">
                            {template.category}
                          </Badge>
                        )}
                      </div>
                      
                      <p className="text-xs text-muted-foreground">
                        {template.description}
                      </p>
                      
                      <div className="space-y-1">
                        <p className="text-xs font-medium">Example:</p>
                        <code className="block text-xs p-2 rounded bg-muted">
                          {template.example}
                        </code>
                      </div>
                      
                      <div className="flex gap-1 flex-wrap">
                        {template.fields.map(field => (
                          <Badge key={field} variant="outline" className="text-xs">
                            {field}
                          </Badge>
                        ))}
                      </div>
                    </div>
                  ))}
                </div>
              </CollapsibleContent>
            </div>
          </Collapsible>
        ))}
      </CardContent>
    </Card>
  )
}