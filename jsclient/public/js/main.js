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

//Function to display the user details on the top right of the webpage
function getUserDetails() {
    var user = sessionStorage.getItem("userId");
    document.getElementById("userCredentials").innerHTML ="Welcome " + user.toUpperCase() + " |";
}  


window.addEventListener("load", function(event) 
{
	getUserDetails();
});

//Validate 
function loginBtnClicked() {
    var userId = document.getElementById('loginId').value;
    if(userId == ""){
        alert("Please Enter User ID");
        window.location.href = '/login';
    } else {
        $.post('/login', { userId: userId },
        function (data, textStatus, jqXHR) {
            if(data.done == 1){
                sessionStorage.clear();
                sessionStorage.setItem("userId" , data.userId);  
                alert(data.message); 
                window.location.href = "/home"; 
            }else{
                alert(data.message); 
                window.location.href = "/login";
            }
            
        },'json');
    }
}

//Successful Logout
function logoutBtnClicked(){
    sessionStorage.clear();
    window.location.href = "/login";
    alert("Successfully Logged out");
}

//Function to deposit money to the specified user account
function depositMoney() {
    var userDetails = sessionStorage.getItem('userId');
    var amount = document.getElementById("depositAmt").value;
    if (amount.length === 0) {
        alert("Please enter some amount");
    } else {
        $.post('/deposit', { userId: userDetails, money: amount },
            function (data, textStatus, jqXHR) {
                window.location.href="/balance";
            },
            'json');
    }
}

//function to withdraw amount from server and display balance
function withdrawMoney() {
    var userDetails = sessionStorage.getItem('userId');
    var amount = document.getElementById("withdrawAmt").value;
    if (amount.length === 0) {
        alert("please enter amount");
    } else {
        $.post('/withdraw', { userId: userDetails, money: amount },
            function (data, textStatus, jqXHR) {
                window.location.href="/balance";
            },
            'json');
    }
}

//function to implement transfer function form client side
function transferMoney() {
    var userDetails = sessionStorage.getItem('userId');
    var beneficiary = document.getElementById('beneficiaryUserId').value;
    var amount = document.getElementById("transferAmt").value;
    if (amount.length === 0) {
        alert("Please enter amount");
	}
    if(beneficiary.length === 0){
        alert("Please Enter the beneficiary"); 
	}
    if(amount.length != 0 && beneficiary.length != 0)){
        $.post('/transfer', { userId: userDetails, beneficiary: beneficiary, money: amount },
            function (data, textStatus, jqXHR) {
                window.location.href="/balance";
            },
            'json');
    }
}

function showBalance() {
    $(".nav").find(".active").removeClass("active");
    $('#balance').addClass("active");    

    var userId = sessionStorage.getItem('userId');
    $.post('/balance', { userId: userId },
         function (data, textStatus, jqXHR) {
              document.getElementById("balanceCheck").innerHTML ="Your balance is:" + "<br />"  + "Rs " + data.balance; 
            },
            'json'); 
}

function homePageLoaded() {
    $(".nav").find(".active").removeClass("active");
    $('#home').addClass("active");    
}


function transferPageLoaded() {
    $(".nav").find(".active").removeClass("active");
    $('#transfer').addClass("active");    
}


function withdrawPageLoaded() {
    $(".nav").find(".active").removeClass("active");
    $('#withdraw').addClass("active");    
}


function depositPageLoaded() {
    $(".nav").find(".active").removeClass("active");
    $('#deposit').addClass("active");    
}
