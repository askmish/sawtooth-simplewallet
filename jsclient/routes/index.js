/**
 * Copyright 2018 Intel Corporation
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
 * ------------------------------------------------------------------------------
 */

var express = require('express');
var bodyParser = require('body-parser');
var router = express.Router();
var {SimpleWalletClient} = require('./SimpleWalletClient') 

var urlencodedParser = bodyParser.urlencoded({ extended: false })

router.get('/', function(req, res){
    res.redirect("/login");
})

//Get home view
router.get('/login', function(req, res){
    res.render('loginPage');
});

//Get main view
router.get('/home', function(req, res){
    res.render('homePage');
});

// Get Deposit view
router.get('/deposit',function(req, res){
    res.render('depositPage');
})

//Get Withdraw view
router.get('/withdraw',function(req, res){
    res.render('withdrawPage');
})

//Get Transfer View
router.get('/transfer',function(req, res){
    res.render('transferPage');
})

//Get Balance View
router.get('/balance', function(req, res){
    res.render('balancePage');
})

//recieve data from login page and save it.
router.post('/login', urlencodedParser, function(req, res){
    var userid = req.body.userId;
    res.send({done:1, userId: userid, message: "User Successfully Logged in as "+userid  });
});

//function to deposit amount in server
router.post('/deposit', function(req, res) {
    var userId = req.body.userId;
    var amount = req.body.money;
    var SimpleWalletClient1 = new SimpleWalletClient(userId); 
    SimpleWalletClient1.deposit(amount);    
    res.send({message:"Amount "+ amount +" successfully added"});
});

//function to withdraw
router.post('/withdraw', function(req, res) {
    var userId = req.body.userId;
    var amount = req.body.money;
    var SimpleWalletClient1 = new SimpleWalletClient(userId);   
    SimpleWalletClient1.withdraw(amount);     
    res.send({  message:"Amount "+ amount +" successfully deducted"});
});

//function to transfer money to another user
router.post('/transfer', function(req, res) {
    var userId = req.body.userId;
    var beneficiary = req.body.beneficiary;
    var amount = req.body.money;
    var client = new SimpleWalletClient(userId);
    client.transfer(beneficiary, amount);    
    res.send({ message:"Amount "+ amount +" successfully added to " + beneficiary});
});

router.post('/balance', function(req, res){
    var userId = req.body.userId;
    var client = new SimpleWalletClient(userId);
    var getYourBalance = client.balance();
    console.log(getYourBalance);
    getYourBalance.then(result => {res.send({ balance: result, message:"Amount " + result + " available"});});
})
module.exports = router;
