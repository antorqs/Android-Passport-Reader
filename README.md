# Android-Passport-Reader
## Implementation of "A Traceability Attack Against e-Passports" paper by T. Chothia and V. Smirnov.l

This Android app is an implementation of the Traceability Attack against e-Passports performed by 
T. Chothia and V. Smirnov. 

In this application, user can test the vulnerability against any e-Passport by registering the correct data 
and perform the attack. It performs a set of continuous attacks in order to estimate the presence of the
vulnerability. This attacks and tests do not represent any risk to the integrity of the passport and/or its
validity.

---
## What is a Traceability Attack?

A traceability attack do not represents a risk to all the information stored in the passport, but however
it represents a real risk to the privacy of the carrier of the document. Being able to identify the carrier
of a specific passport, and detect his or her presence in a given location can be used with evil intentions,
like detonating a bomb, for example.

According to the ICAO, all passports must respond to any message received, returning an error code if the
message was incorrect or unexpected. In a specific case, the French passports present a vulnerability in the
BAC protocol due to the error messages it returns depending on the inquiry being made.

If a passport is queried with a wrong MAC, it responds with a code "6300 - No information given". On the other 
hand, if a right MAC is given but the random number NT is incorrect, the answer would be "6A80 Incorrect 
parameters". 

To be able to exploit this vulnerability, the attacker must "listen" a successful communication between the 
passport and a reader (in an airport, for example); and store a correct message. Later, in order to try to 
identify it, the attacker must re-send the stored message; if it gets a "6300" response, it knows that the
MAC was calculated with wrong data, which means that it is not the same passport, BUT, if it gets a "6A800"
as response, it means that the MAC was correct but the random number is incorrect; this indicates that
it has communicated with the same passport and it has been spotted.

As there are countries that do not have this vulnerability, due to the fact that, for both cases the 
passport returns a "6300" message; Chothia and Smirnov have proposed another attacked based on the 
response times.

This attacked is based on the idead that, the passport sends a response to a wrong MAC faster than it does
to a right MAC and a wrong random number. In other words, if the attacker manages to store a correct 
message,as explained previously, then it has to send a wrong message (wrong MAC) to the passport and 
measure the response time, and then it re-sends the stored (correct) message and measure the response time. 
If both times are different, then the passport has been spotted.

This phenomenon occurs due to the fact that the passport (theoretically) first verifies the MAC and, if 
it's correct it verifies the content of the message; so, we have two scenarios: a) The passport receives 
a wrong MAC, detects it and sends a response immediately, cancelling any further computation, in a time T1;
b) The MAC is correct, so the passport continues its procedure and verifies the message's content, where it
determines that it is incorrect so it sends a response in a time T2. Theoretically, if T2 is greater than T1
(Due to the adicional computation of verifying the message's content) the passport is vulnerable.

In this application, both attacks can be performed in order to determine if a passport is vulnerable or not.
