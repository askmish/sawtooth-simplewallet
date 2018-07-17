/*
 * Copyright 2018 Intel Corporation.
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

use crypto::digest::Digest;
use crypto::sha2::Sha512;

use std::str;
use std::collections::HashMap;

use sawtooth_sdk::processor::handler::ApplyError;
use sawtooth_sdk::processor::handler::TransactionContext;

pub fn get_sw_prefix() -> String {
        let mut sha = Sha512::new();
        sha.input_str("simplewallet");
        sha.result_str()[..6].to_string()
}

//Simplewallet State
pub struct SwState<'a> {
    context: &'a mut TransactionContext,
}

impl<'a> SwState<'a> {
    pub fn new(context: &'a mut TransactionContext) -> SwState {
        SwState {
            context: context,
        }
    }

    fn calculate_address(name: &str) -> String {
        let mut sha = Sha512::new();
        sha.input_str(name);
        get_sw_prefix() + &sha.result_str()[..64].to_string()
    }
    
    pub fn get(&mut self, name: &str) -> Result<Option<u32>, ApplyError> {
        let address = SwState::calculate_address(name);
        let d = self.context.get_state(vec![address.clone()])?;
        match d {
            Some(packed) => {                
                
                let value_string = match String::from_utf8(packed) {
                                      Ok(v) => v,
                                      Err(_) => return Err(ApplyError::InvalidTransaction(String::from(
                                                              "Invalid UTF-8 sequence")))
                                   };                
                
                let value: u32 = match value_string.parse() {
                                    Ok(v) => v,
                                    Err(_) => return Err(ApplyError::InvalidTransaction(String::from(
                                                              "Unable to parse UTF-8 String as u32")))
                                 };
                
                Ok(Some(value))
                               
            }
            None => Ok(None),
        }
    }

    pub fn set(&mut self, name: &str, value: u32) -> Result<(), ApplyError> {
       
        let mut sets = HashMap::new();
        sets.insert(SwState::calculate_address(name), value.to_string().into_bytes());
        self.context
            .set_state(sets)
            .map_err(|err| ApplyError::InternalError(format!("{}", err)))?;

        Ok(())
    }
}    