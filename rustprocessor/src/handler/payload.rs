/*
 * Copyright 2018 Intel corporation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -----------------------------------------------------------------------------
 */

use std::str;
use std::fmt;

use sawtooth_sdk::processor::handler::ApplyError;

#[derive(Copy, Clone)]
pub enum Action {
    Deposit,
    Withdraw,
    Balance,
    Transfer,
}

impl fmt::Display for Action {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        write!(
            f,
            "{}",
            match *self {
                Action::Deposit => "Action::Deposit",
                Action::Withdraw => "Action::Withdraw",
                Action::Balance => "Action::Balance",
                Action::Transfer => "Action::Transfer",
            }
        )
    }
}


pub struct SwPayload {
    action: Action,    
    value: u32,
    beneficiary_pubkey: Option<String>,
}

impl SwPayload {

    pub fn new(payload_data: &[u8]) -> Result<Option<SwPayload>, ApplyError> {
    
        let payload_string = match str::from_utf8(&payload_data) {
            Ok(s) => s,
            Err(_) => {
                return Err(ApplyError::InvalidTransaction(String::from(
                    "Invalid payload serialization",
                )))
            }
        };

        //SimpleWallet payload is constructed as comma separated items
        let items: Vec<&str> = payload_string.split(",").collect();

        if items.len() < 2 {
            return Err(ApplyError::InvalidTransaction(String::from(
                "Payload must have at least 1 comma",
            )));
        }
        
        if items.len() > 3 {
            return Err(ApplyError::InvalidTransaction(String::from(
                "Payload must have at most 2 commas",
            )));
        }
                
        let (action, value) = (items[0], items[1]);
        
        if action.is_empty() {
            return Err(ApplyError::InvalidTransaction(String::from(
                "Action is required",
            )));
        }
             
        
        let action = match action {
                            "deposit" => Action::Deposit,
                            "withdraw" => Action::Withdraw,
                            "balance" => Action::Balance,
                            "transfer" => Action::Transfer,
                            _ => {
                                return Err(ApplyError::InvalidTransaction(String::from(
                                    "Invalid Action",
                                )))
                          }
        };
 
         
        let value: u32 = match value.parse() {
            Ok(num) => num,
            Err(_) => {
                return Err(ApplyError::InvalidTransaction(String::from(
                    "Missing integer value",
                )))
            }
        };        
        
        let mut beneficiary_pubkey = None;
         
        if items.len() == 3  {
                    
            if items[2].is_empty() {
                return Err(ApplyError::InvalidTransaction(String::from(
                    "Beneficiary cannot be empty ",
                )));
            }
            
            beneficiary_pubkey = Some(items[2].to_string());
            
        }
        
        
        let payload = SwPayload {
            action: action,
            value: value,
            beneficiary_pubkey: beneficiary_pubkey,
        };
        
        Ok(Some(payload))                       
    }
    
    pub fn get_action(&self) -> Action {
        self.action
    }
    
    pub fn get_value(&self) -> u32 {
        self.value
    }
    
    pub fn get_beneficiary(&self) -> &Option<String> {
        
        &self.beneficiary_pubkey      
    }

}
