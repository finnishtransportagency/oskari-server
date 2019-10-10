Oskari.registerLocalization(
{
    "lang": "fi",
    "key": "MyPlacesImport",
    "value": {
        "title": "Omat aineistot",
        "desc": "",
        "tool": {
            "tooltip": "Omien aineistojen tuonti"
        },
        "flyout": {
            "title": "Omien aineistojen tuonti",
            "description": "Lataa aineisto tietokoneeltasi yhdeksi zip-tiedostoksi pakattuna, joka sisältää tarvittavat tiedostot jostain seuraavasta tiedostomuodosta:<ul><li>Shapefile (.shp, .shx, .dbf ja .prj sekä mahdollinen.cpg)</li><li>GPX-siirtotiedosto (.gpx)</li><li>MapInfo (.mif ja .mid)</li><li>Google Map (.kml)</li></ul>Zip-tiedosto saa sisältää vain yhden karttatason ja sen maksimikoko on {maxSize, number} Mt.",
            "help": "Lataa aineisto tietokoneeltasi pakattuna zip-tiedostoon. Tarkista, että aineisto on oikeassa tiedostomuodossa ja koordinaattijärjestelmässä.",
            "actions": {
                "cancel": "Peruuta",
                "next": "Seuraava",
                "close": "Sulje",
                "submit": "Tuo aineisto"
            },
            "layer": {
                "title": "Tallenna karttatason tiedot:",
                "name": "Karttatason nimi",
                "desc": "Kuvaus",
                "source": "Tietolähde",
                "style": "Tyylimäärittelyt",
                "srs": "EPSG-koodi"
            },
            "validations": {
                "error": {
                    "title": "Virhe",
                    "message": "Aineiston tuonti epäonnistui. Huomioi seuraavat puutteet ja yritä sitten uudelleen.",
                    "name": "Karttatasolla pitää antaa nimi.",
                    "file": "Valitse aineiston sisältävä tiedosto.",
                    "epsg": "EPSG-koodi pitää antaa numerona"
                }
            },
            "finish": {
                "success": {
                    "title": "Aineiston tuonti onnistui",
                    "message": "Aineistosta tuotiin {count, plural, one {# kohde} other {# kohdetta}} onnistuneesti. Voit tarkastella aineistoa Omat tiedot -valikon Omat aineistot -välilehden kautta."
                },
                "failure": {
                    "title": "Aineiston tuonti epäonnistui"
                }

            },
            "error":{
                "title": "Aineiston tuonti epäonnistui",
                "timeout":"Aineiston tuonti keskeytyi aikakatkaisun vuoksi.",
                "abort": "Aineiston tuonti keskeytettiin.",
                "generic": "Järjestelmässä tapahtui tunnistamaton virhe.",
                "hasFolders": "Huom! Tarkista, ettei aineiston tiedostot ole zip-tiedostossa kansion sisällä.",

                // unknown_projection
                "noSrs": "Tiedostosta ei pystytty tunnistamaan koordinaattijärjestelmää. Varmista, että aineistossa on koordinaattijärjestelmä määritetty oikein tai anna EPSG-koodi numerona (esim. 4326) tekstikenttään. Yritä sen jälkeen uudelleen.",
                "shpNoSrs": "SHP-tiedostosta ei pystytty tunnistamaan koordinaattijärjestelmää. Pakkaa tiedostoon mukaan koordinaattijärjestelmän määrittävä prj-tiedosto tai anna EPSG-koodi numerona (esim. 4326) tekstikenttään. Yritä sen jälkeen uudelleen.",

                // parser_error
                "parserGeneric": "Aineiston jäsentämisessä tapahtui tunnistamaton virhe.",
                "kml":"Karttatasoa ei onnistuttu luomaan KML-tiedostosta.",
                "shp":"Karttatasoa ei onnistuttu luomaan SHP-tiedostosta.",
                "mif":"Karttatasoa ei onnistuttu luomaan MIF-tiedostosta.",
                "gpx":"Karttatasoa ei onnistuttu luomaan GPX-tiedostosta.",

                // Error codes from backend
                "no_main_file":"Järjestelmän tukemaa tiedostoa ei löytynyt. Tarkista, että käyttämäsi tiedostomuoto on tuettu ja aineisto on pakattuna zip-tiedostoon.",
                "too_many_files": "Zip-tiedosto sisälsi ylimääräisiä tiedostoja. Poista ylimääräiset tiedostot ja jätä vain tarvittavat ohjeiden mukaiset tiedostot.",
                "multiple_extensions": "Tiedostosta löytyi useita samalla {extension}-tiedostopäätteellä olevia tiedostoja. Tiedosto voi sisältää vain yhden aineiston tiedostot.",
                "multiple_main_file": "Tiedostosta löytyi useita eri aineistoja ({extensions}). Tiedosto voi sisältää vain yhden aineiston tiedostot.",
                "unable_to_store_data":"Aineiston kohteita ei voitu tallentaa. Tarkista, että kaikki tiedostomuodon tarvitsemat tiedostot ovat zip-tiedostossa sekä tarkista ettei aineiston kohdetiedot ole virheellisiä.",
                //"short_file_prefix":"Zip-tiedostosta ei onnistuttu hakemaan aineiston tiedostoja. Tarkista, ettei pakattujen tiedostojen nimet ole alle kolmen merkin pituisia.",
                "file_over_size":"Valitsemasi tiedosto on liian suuri. Enimmäiskoko on {maxSize, number} Mt.",
                "no_features":"Aineistosta ei löytynyt kohdetietoja. Tarkista, että aineiston kohdetiedoilla on koordinaatit määritetty.",
                //"malformed":"Tarkista, ettei tiedostonimissä ole käytetty ääkkösiä.",
                "invalid_epsg": "Syöttämääsi EPSG-koodia ei tunnistettu. Tarkasta, että se on oikein ja numeromuodossa (esim. 4326). Jos koodia ei tästä huolimatta tunnisteta, niin koordinaattijärjestelmän tiedot pitää lisätä aineistoon.",
                "format_failure": "Tuotu aineisto ei ole kelvollinen. Tarkasta aineiston oikeellisuus ja yritä uudelleen."
            },
            "warning":{
                "features_skipped":"Huomio! Aineiston tuonnissa {count, plural, one {# kohde} other {# kohdetta}} hylättiin puuttuvien tai viallisten koordinaattien tai geometrian vuoksi."
            }
        },
        "tab": {
            "title": "Aineistot",
            "editLayer": "Muokkaa karttatasoa",
            "deleteLayer": "Poista karttataso",
            "grid": {
                "name": "Nimi",
                "description": "Kuvaus",
                "source": "Tietolähde",
                "edit": "Muokkaa",
                "editButton": "Muokkaa",
                "remove": "Poista",
                "removeButton": "Poista"
            },
            "confirmDeleteMsg": "Haluatko poistaa aineiston \"{name}\"?",
            "buttons": {
                "ok": "OK",
                "save": "Tallenna",
                "cancel": "Peruuta",
                "delete": "Poista",
                "close": "Sulje"
            },
            "notification": {
                "deletedTitle": "Poista aineisto",
                "deletedMsg": "Aineisto on poistettu.",
                "editedMsg": "Aineisto on päivitetty."
            },
            "error": {
                "title": "Virhe",
                "generic": "Aineiston lataus epäonnistui järjestelmässä tapahtuneen virheen vuoksi.",
                "deleteMsg": "Aineiston poistaminen epäonnistui järjestelmässä tapahtuneen virheen vuoksi.",
                "editMsg": "Aineiston päivitys epäonnistui järjestelmässä tapahtuneen virheen vuoksi. Yritä myöhemmin uudelleen.",
                "getStyle": "Aineistolle määritettyä tyyliä ei onnistuttu hakemaan. Lomakkeella näytetään oletusarvot. Vaihda tyylimäärittelyn arvot ennen muutosten tallennusta.",
                "styleName": "Anna karttatasolle nimi ja yritä sitten uudelleen."
            }
        },
        "layer": {
            "organization": "Omat aineistot",
            "inspire": "Omat aineistot"
        }
    }
});
