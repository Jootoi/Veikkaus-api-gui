#Graafinen käyttöliittymä Veikkauksen API:lle

Ohjelma mahdollistaa joidenkin [Veikkauksen API](https://github.com/VeikkausOy/sport-games-robot):n antamien tietojen selaamisen graafisessa muodossa.
Toistaiseksi ohjelma näyttää vain Toto peleihin liittyviä tietoja.

##Miksi?

Ohjelman tarkoituksena on toimia laajenevana harjoitustyönä JSON APIsta, http yhteyksistä, graaffisista käyttöliittymistä, säikeistyksestä ja yleisesti Java ohjelmoinnista.
Samalla ohjelma voi toimia esimerkkinä jos haluaa toteuttaa [Veikkauksen API](https://github.com/VeikkausOy/sport-games-robot) sivulla kuvatun kaltaisen pelirobotin Javalla. 

##Asentaminen

###Tapa 1: Lataa valmiit tiedostot

Ohjelma ja sen vaatimat kirjastot löytyvät käyttövalmiina [7zip pakkauksena releases sivulta](https://github.com/Jootoi/Veikkaus-api-gui/releases/tag/1.0)
Pura pakkaus halumaasi kansioon sopivaa ohjelmistoa käyttäen.

###Tapa 2: Git + Maven

Avaa komentorivillä kansio johon haluat tallettaa tiedostot. Ladataksesi lähdekoodi tiedostot kirjoita komentoriville:
```
git clone https://github.com/Jootoi/Veikkaus-api-gui.git
```
Ohjelma käännetään ja vaadittavat kirjastot ladataan komenolla:
```
mvn clean install
```


##Käyttö

Käynnistä lataamasi/itse kääntämäsi veikkaus-api-gui-1.0.jar tiedosto valitsemallasi tavalla.
Käynnistyksen jälkeen esiin kuuluisi tulla käyttöliittymä, jonka kautta tietoja on mahdollist selata kunhan ohjelma saa kaiken tarvitsemansa ladattua.
Käyttöliittymän Toto välilehdeltä löytyy kaikki päivän pelikohteet jaoteltuna paikkakunnan ja lähdön mukaan.
Debug välilehdeltä voi seurata tarkemmin ohjelman toimintaa, sekä sammuttaa/käynnistää APIn tietojen hakemisen.

##TODO
Asioita joita tulen ehkä aikanaan lisäämään ohjelmaan:

+Mahdollisuus pelata Veikkauksen pelejä
+Tietoa muistakin kuin toto kohteista
+Mahdollisuus vaihtaa käyttöliittymän kieli
+Nopeampi tietojen haku API:sta (API sallii viiden säikeen ajamisen)

##Lisenssi

Copyright (c) 2017, Joonas Toimela
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

Ohjelman käyttämät ulkopuoliset kirjastot (Google Gson, OkHttp, okio) ovat [Apache 2.0 lisenssin](https://www.apache.org/licenses/LICENSE-2.0) alaisia.