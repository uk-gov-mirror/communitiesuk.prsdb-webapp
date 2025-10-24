-- This script populates the database with n^2 landlords, where 'n' is the cardinality of the name array passed to load_search_data().
-- 2 property ownerships are generated per landlord (one with a license, one without).
-- By default, 1580 names are passed to load_search_data() so 2,496,401 landlords and 4,992,802 property ownerships are generated.

CREATE OR REPLACE FUNCTION generate_registration_numbers(minNumber BIGINT, desiredCount INT)
RETURNS BIGINT[]
LANGUAGE plpgsql
AS $$
DECLARE
    maxRegistrationNumberCount BIGINT := 208827064576;
    availableRegistrationNumberCount BIGINT;

    registrationNumbers BIGINT[];
    outOfSeriesNumber BIGINT := minNumber + desiredCount + 1;
BEGIN
    SELECT maxRegistrationNumberCount - count(*) FROM registration_number INTO availableRegistrationNumberCount;
    IF desiredCount > availableRegistrationNumberCount THEN
        RAISE EXCEPTION '% numbers desired, but only % are free', desiredCount, availableRegistrationNumberCount;
    END IF;

    SELECT array_agg(number)
    FROM (SELECT *
          FROM generate_series(minNumber, minNumber + desiredCount) AS series(number)
          WHERE NOT exists(SELECT 1 FROM registration_number r WHERE r.number = series.number)
         ) numbers
    INTO registrationNumbers;

    WHILE cardinality(registrationNumbers) < desiredCount LOOP
        IF NOT exists(SELECT 1 FROM registration_number r WHERE r.number = outOfSeriesNumber) THEN
            registrationNumbers := array_append(registrationNumbers, outOfSeriesNumber);
        END IF;
        outOfSeriesNumber := outOfSeriesNumber + 1;
    END LOOP;

    RETURN registrationNumbers;
END;
$$;

CREATE OR REPLACE FUNCTION get_unused_address_ids(desiredCount INT)
RETURNS BIGINT[]
LANGUAGE plpgsql
AS $$
DECLARE
    unusedAddressIds BIGINT[];
BEGIN
    SELECT array_agg(id)
    FROM (SELECT a.id
          FROM address a
          WHERE NOT exists(SELECT 1 FROM property p WHERE p.address_id = a.id)
          AND a.local_authority_id IS NOT NULL
          LIMIT desiredCount
         ) ids
    INTO unusedAddressIds;

    IF cardinality(unusedAddressIds) < desiredCount THEN
        RAISE EXCEPTION '% addresses desired, but only % are free', desiredCount, cardinality(unusedAddressIds);
    END IF;

    RETURN unusedAddressIds;
END;
$$;

CREATE OR REPLACE FUNCTION generate_email(firstName TEXT, secondName TEXT)
RETURNS TEXT
LANGUAGE plpgsql
AS $$
DECLARE
    separator TEXT := (ARRAY['', '_', '.', '-'])[floor(random() * 4)::int + 1];
    domain TEXT := (ARRAY['@example.com', '@example.net', '@example.org', '@test.com', '@test.net', '@test.org'])[floor(random() * 6)::int + 1];
BEGIN
    RETURN quote_literal(firstName || separator || secondName || domain);
END;
$$;

CREATE OR REPLACE FUNCTION generate_phone_number()
RETURNS TEXT
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN quote_literal('07' || lpad((floor(random() * 1000000000))::text, 9, '0'));
END;
$$;

CREATE OR REPLACE FUNCTION load_search_data_batch(oneLoginUsers TEXT, registrationNumbers TEXT, landlords TEXT, properties TEXT, licenses TEXT, propertyOwnerships TEXT)
RETURNS VOID
LANGUAGE plpgsql
AS $$
BEGIN
    EXECUTE format('INSERT INTO one_login_user (id) VALUES %s;', trim(TRAILING ',' FROM oneLoginUsers));
    EXECUTE format('INSERT INTO registration_number (id, number, type) VALUES %s;', trim(TRAILING ',' FROM registrationNumbers));
    EXECUTE format(
        'INSERT INTO landlord (id, subject_identifier, name, email, phone_number, address_id, country_of_residence, registration_number_id) VALUES %s;',
        trim(TRAILING ',' FROM landlords)
            );
    EXECUTE format('INSERT INTO property (id, status, is_active, property_build_type, address_id) VALUES %s;', trim(TRAILING ',' FROM properties));
    EXECUTE format('INSERT INTO license (id, license_type, license_number) VALUES %s;', trim(TRAILING ',' FROM licenses));
    EXECUTE format(
        'INSERT INTO property_ownership (is_active, occupancy_type, ownership_type, registration_number_id, primary_landlord_id, property_id, license_id) VALUES %s;',
        trim(TRAILING ',' FROM propertyOwnerships)
            );
END;
$$;

CREATE OR REPLACE FUNCTION load_search_data(nameHalves TEXT[])
RETURNS VOID
LANGUAGE plpgsql
AS $$
DECLARE
    nameCount INT := power(cardinality(nameHalves), 2);
    batchSize INT := least(nameCount, 5000);

    firstName TEXT;
    secondName TEXT;
    nameIndex INT;

    landlordBaseUserId TEXT;
    oneLoginUsers TEXT := '';

    lrnId BIGINT;
    prnId BIGINT;
    unusedRegistrationNumbers BIGINT[] = generate_registration_numbers(0, batchSize * 3);
    unusedRegistrationNumberIndex INT := 1;
    registrationNumbers TEXT := '';

    landlordId BIGINT;
    landlordAddressId BIGINT;
    landlords TEXT := '';

    propertyId BIGINT;
    unusedAddressIds BIGINT[] = get_unused_address_ids(batchSize * 2);
    unusedAddressIdIndex INT := 1;
    properties TEXT := '';

    licenseId BIGINT;
    licenses TEXT := '';

    propertyOwnerships TEXT := '';
BEGIN
    SELECT coalesce(max(id), 0) + 1 FROM registration_number INTO lrnId;
    prnId := lrnId + 1;

    SELECT min(id) FROM address INTO landlordAddressId;
    SELECT coalesce(max(id), 0) + 1 FROM landlord INTO landlordId;

    SELECT coalesce(max(id), 0) + 1 FROM property INTO propertyId;
    SELECT coalesce(max(id), 0) + 1 FROM license INTO licenseId;

    FOR firstName, secondName, nameIndex IN
        SELECT *, row_number() OVER () as nameIndex FROM unnest(nameHalves) AS firstName, unnest(nameHalves) AS secondName
    LOOP
        landlordBaseUserId := quote_literal('urn:fdc:gov.uk.eg:2025:' || landlordId);
        oneLoginUsers := oneLoginUsers || '(' || landlordBaseUserId || '),';

        registrationNumbers := registrationNumbers || '(' || concat_ws(',', lrnId, unusedRegistrationNumbers[unusedRegistrationNumberIndex], 1) || '),';
        unusedRegistrationNumberIndex := unusedRegistrationNumberIndex + 1;

        landlords := landlords || '(' || concat_ws(',',
                                                   landlordId,
                                                   landlordBaseUserId,
                                                   quote_literal(firstName || ' ' || secondName),
                                                   generate_email(firstName, secondName),
                                                   generate_phone_number(),
                                                   landlordAddressId,
                                                   quote_literal('England or Wales'),
                                                   lrnId
                                         ) || '),';

        FOR propertyIndex IN 1..2 LOOP
            registrationNumbers := registrationNumbers || '(' || concat_ws(',', prnId, unusedRegistrationNumbers[unusedRegistrationNumberIndex], 0) || '),';
            unusedRegistrationNumberIndex := unusedRegistrationNumberIndex + 1;

            properties := properties || '(' || concat_ws(',', propertyId, 1, 'true', 0, unusedAddressIds[unusedAddressIdIndex]) || '),';
            unusedAddressIdIndex := unusedAddressIdIndex + 1;

            IF propertyIndex = 1 THEN
                licenses := licenses || '(' || concat_ws(',', licenseId, floor(random() * 3), quote_literal('123456')) || '),';
                propertyOwnerships := propertyOwnerships || '(' || concat_ws(',', 'true', 0, 0, prnId, landlordId, propertyId, licenseId) || '),';
                licenseId := licenseId + 1;
            ELSE
                propertyOwnerships := propertyOwnerships || '(' || concat_ws(',', 'true', 0, 0, prnId, landlordId, propertyId, 'null') || '),';
            END IF;

            prnId := prnId + propertyIndex; -- Skip 1 to avoid clashes with LRNs
            propertyId := propertyId + 1;
        END LOOP;

        lrnId := lrnId + 3; -- Skip 2 to avoid clashes with PRNs
        landlordId := landlordId + 1;

        IF nameIndex % batchSize = 0 OR nameIndex = nameCount THEN
            PERFORM load_search_data_batch(oneLoginUsers,
                                           registrationNumbers,
                                           landlords,
                                           properties,
                                           licenses,
                                           propertyOwnerships);

            oneLoginUsers := '';
            registrationNumbers := '';
            landlords := '';
            properties := '';
            licenses := '';
            propertyOwnerships := '';

            batchSize := least(batchSize, nameCount - nameIndex);

            unusedRegistrationNumbers := generate_registration_numbers(unusedRegistrationNumbers[cardinality(unusedRegistrationNumbers)], batchSize * 3);
            unusedRegistrationNumberIndex := 1;

            unusedAddressIds := get_unused_address_ids(batchSize * 2);
            unusedAddressIdIndex := 1;
        END IF;

        IF nameIndex % 10000 = 0 OR nameIndex = nameCount THEN
            RAISE NOTICE '[%] Inserted % landlords and % property ownerships', clock_timestamp(), nameIndex, nameIndex * 2;
        END IF;
    END LOOP;

    PERFORM setval(pg_get_serial_sequence('registration_number', 'id'), (SELECT max(id) FROM registration_number));
    PERFORM setval(pg_get_serial_sequence('landlord', 'id'), (SELECT max(id) FROM landlord));
    PERFORM setval(pg_get_serial_sequence('property', 'id'), (SELECT max(id) FROM property));
    PERFORM setval(pg_get_serial_sequence('license', 'id'), (SELECT max(id) FROM license));
END;
$$;

DO $$
BEGIN
    PERFORM load_search_data(ARRAY[
        'Otto', 'Baxter', 'Harvey', 'Mikhail', 'Lewis', 'Cai', 'Ami', 'Phoevos', 'Bader', 'Karol',
        'Ibrahim', 'Noor', 'Frank', 'Elliott', 'Hubert', 'Rayan', 'Nihaal', 'Kori', 'Sajjad', 'Stuart',
        'Devlin', 'Euan', 'Valen', 'Robi', 'Spencer', 'Umair', 'Arnab', 'Matthew-William', 'Brodi', 'Guthrie',
        'Ruadhan', 'Cal', 'Baillie', 'Maryk', 'Conley', 'Dalong', 'Leven', 'Asim', 'Sidney', 'Kale',
        'Johansson', 'Abdisalam', 'Finley', 'Darrius', 'Toluwalase', 'Antonio', 'Argyle', 'Raithin', 'Marshall', 'Leilan',
        'Marcous', 'Caolain', 'Mohamad', 'Justan', 'Kendyn', 'Ubaid', 'Kristopher', 'Cesare', 'Aulay', 'Ronin',
        'Fox', 'Patrick', 'Dhavid', 'Lachlainn', 'Jahy', 'Elijah', 'Habeeb', 'Maxx', 'Ammar', 'Jayson',
        'Alber', 'Wayne', 'Lawson', 'Alhaji', 'Charly', 'Cain', 'Lorenz', 'Dane', 'Bradley', 'Owyn',
        'Tyler', 'Marcos', 'Bobby-Lee', 'Anton', 'Amro', 'Derren', 'Sukhvir', 'Ishwar', 'Brannan', 'Tokinaga',
        'Eliot', 'Lucas', 'Johannes', 'James-Paul', 'Pedram', 'Coben', 'Niro', 'Jay-Jay', 'Steven-lee', 'Devin',
        'Fyn', 'Gil', 'Shazil', 'Momin', 'Balian', 'Tymon', 'Grant', 'Cale', 'Franko', 'Kalvin',
        'Famara', 'Preston', 'Aydan', 'Drew', 'Anthony-John', 'Kael', 'Connel', 'Conlin', 'Aydin', 'Darius',
        'Sohan', 'Kaydan', 'Izaak', 'Ahmed-Aziz', 'Dominik', 'Nicol', 'Forrest', 'Xander', 'Lomond', 'Marc',
        'Pietro', 'Chester', 'Keiryn', 'Amin', 'Samy', 'Lochlan-Oliver', 'Kyel', 'Afonso', 'Nayan', 'Aiden',
        'Manson', 'Peiyan', 'Baye', 'Jarno', 'Ewan', 'Christoph', 'Christopher', 'Chi', 'Kajally', 'Ghyll',
        'Liall', 'Pearce', 'Forbes', 'Jaydn', 'Kym', 'Jayden-Lee', 'Maias', 'Lockey', 'Shaunpaul', 'Garry',
        'Hristomir', 'Shawnpaul', 'Dilraj', 'Gytis', 'Kacey', 'Conlan', 'Ty', 'Lionel', 'Innes', 'Eoghan',
        'Youssef', 'Mark', 'Lloyde', 'Seamas', 'Flynn', 'Kelvan', 'Kyrran', 'Adam-James', 'Clayton', 'Kristoffer',
        'Campbel', 'Harry', 'Aslam', 'Seb', 'Douglas', 'Rhys', 'Abdulmalik', 'Jaise', 'Hong', 'Demetrius',
        'Arron', 'Lance', 'Keayn', 'Norman', 'Dale', 'Keavan', 'Harlee', 'Ciann', 'Awwal', 'Luca',
        'Lauchlan', 'Manmohan', 'Atapattu', 'Levy', 'Kaelin', 'Daryl', 'Ardal', 'Braydyn', 'Donald', 'Maitlind',
        'Issiaka', 'Zakariya', 'Zeid', 'Rafael', 'Lepeng', 'Gio', 'Kyhran', 'Girius', 'Camron', 'Anees',
        'Finnan', 'Cesar', 'Codie', 'Sol', 'Cian', 'Philippos', 'Jahid', 'Kashif', 'Sorley', 'Niraj',
        'Marlon', 'Jeremy', 'Armaan', 'Hagun', 'Pearse', 'Mack', 'Konrad', 'Peirce', 'Wai', 'Maciej',
        'Emanuel', 'Jaden', 'Jakob', 'Yoolgeun', 'Qasim', 'Aled', 'Aria', 'Jon', 'Teydren', 'Mikee',
        'Oban', 'Aonghus', 'Harish', 'Harris', 'Sylvain', 'Rexford', 'Raja', 'Etienne', 'Daniel', 'Caidyn',
        'Chris', 'Arandeep', 'Kalvyn', 'Blake', 'Evann', 'Milo', 'Giacomo', 'Meyzhward', 'Muneeb', 'Hendri',
        'Aarman', 'Ilyas', 'Kole', 'Teodor', 'Timucin', 'Gurthar', 'Jazib', 'Codey', 'Jaii', 'Grayson',
        'Cayden-Robert', 'Lincon', 'Korbyn', 'Urban', 'Zane', 'Muhammed', 'Jarvi', 'Harrington', 'Finlay', 'Hishaam',
        'Faheem', 'Logan', 'Georgia', 'Moray', 'Aidan', 'Davie', 'Joaquin', 'Muhammad', 'Baron', 'Kimi',
        'Darrach', 'Allan-Laiton', 'Caiden', 'Khevien', 'Keith', 'Kensey', 'Logan-Rhys', 'Keiren', 'Cahlum', 'Immanuel',
        'Zakaria', 'Juan', 'Marcus', 'Rayaan', 'Raza', 'Zac', 'Chibudom', 'Vasyl', 'Leo', 'Jia',
        'Albie', 'Nagib', 'Darwyn', 'Ian', 'Qirui', 'Esteban', 'Zaak', 'Kylar', 'Tristan', 'Ramanas',
        'Jensyn', 'Derick', 'Liyonela-Elam', 'Alphonse', 'Ray', 'Kurtis-Jae', 'Raith', 'Tylor', 'Patryk', 'Eihli',
        'Orley', 'Samual', 'Denver', 'Husnain', 'Karandeep', 'Reese', 'Comghan', 'Heini', 'Farhan', 'Ismail',
        'Azeem', 'Basher', 'Jomuel', 'Muhamadjavad', 'Mathew', 'Adegbola', 'Tubagus', 'Dylan-Patrick', 'Jordon', 'Danar',
        'Jesse', 'Louie', 'Connar', 'Cailean', 'Antony', 'Braeden', 'Brunon', 'Malo', 'Sameer', 'Lagan',
        'Decklan', 'Mehmet', 'Airidas', 'Marcello', 'Richie', 'Abdirahman', 'Umut', 'Saffi', 'Kiegan', 'Tjay',
        'Peregrine', 'Jedd', 'Darrell', 'Cosmo', 'Woyenbrakemi', 'Sofian', 'Lucus', 'Macsen', 'Rico', 'Rio',
        'Tristain', 'Leroy', 'Dareh', 'Klein', 'Uchenna', 'Torran', 'Naif', 'Ehsan', 'Porter', 'Xue',
        'Ceilan', 'Ramit', 'Tiago', 'Addisson', 'Mateusz', 'Presley', 'Markus', 'Kameron', 'Kyle', 'Garrett',
        'Mitchel', 'Jayke', 'Roshan', 'Kiyonari', 'Aref', 'Rajab-Ali', 'Miles', 'Wabuya', 'Erik', 'Danial',
        'Rob', 'Santino', 'Seumas', 'Ken', 'Arya', 'Sergio', 'Mashhood', 'Brogan', 'Jason', 'Guang',
        'Ines', 'Elvin', 'Bezalel', 'Daniyal', 'Brooklyn', 'Suheyb', 'John-Michael', 'Bowie', 'Richey', 'Finnlay',
        'Remo', 'Suraj', 'Michael-Alexander', 'Aiman', 'Heidar', 'Jeronimo', 'Piotr', 'Angelo', 'Jake', 'Zhen',
        'Denon', 'Abdulkadir', 'Barkley', 'Sethu', 'Omri', 'Jeevan', 'Kieryn', 'Breogan', 'Preston-Jay', 'Lochlann',
        'Zubair', 'Iman', 'Rihonn', 'Mickey', 'Sulayman', 'Dilano', 'Khizar', 'Talon', 'Wasif', 'Jameel',
        'Sambrid', 'Ediomi', 'Aryian', 'Kuba', 'Tegan', 'Darien', 'Diego', 'Linden', 'Nick', 'Sebastien',
        'Emmet', 'Eduardo', 'Zachariah', 'Tee-jay', 'Alvin', 'Munir', 'Kynan', 'Bryce', 'Benedict', 'Mirza',
        'Karimas', 'Andrejs', 'Makin', 'Stewarty', 'Chris-Daniel', 'Binod', 'Zein', 'Ajayraj', 'Aron', 'Faizaan',
        'Rorie', 'Kayden', 'Konan', 'Alister', 'Parker', 'Matas', 'Emile', 'Wiktor', 'Bjorn', 'Robin',
        'Arnold', 'Arayan', 'Chu', 'Ed', 'Havila', 'Isaac', 'Tamiem', 'Dimitri', 'Samarjit', 'Shayne',
        'Paul', 'Forgan', 'Callum', 'Jacky', 'Murdo', 'Leydon', 'Bailee', 'Yassin', 'Alishan', 'Luka',
        'Franciszek', 'Deklain-Jaimes', 'Sahbian', 'Rhyse', 'Tiree', 'Kenzie', 'Eonan', 'Litrell', 'Jole', 'Oluwafemi',
        'Hamza', 'Brandonlee', 'Ellisandro', 'Idahosa', 'Michael-James', 'Jevan', 'Vladimir', 'Dafydd', 'Bryson', 'Keivlin',
        'Tiylar', 'Bartosz', 'Layne', 'Darroch', 'Calder', 'Kirk', 'Amgad', 'Pranav', 'Sayf', 'Oluwafikunayomi',
        'Lucian', 'Xabier', 'Teo', 'Kobe', 'Aden', 'Lliam', 'Meshach', 'Karthikeya', 'Ethan', 'Marlin',
        'Sher', 'Allesandro', 'Coel', 'Antoni', 'Kames', 'Seane', 'Aedan', 'Rossi', 'Trent', 'Alfred',
        'Conlon', 'Blyth', 'Lucien', 'Oisin', 'Allen', 'Zhong', 'Conal', 'Savin', 'Brooklin', 'Abir',
        'Geordie', 'Jaskaran', 'Eng', 'Mohd', 'Rupert', 'Jadyn', 'Leyton', 'Trai', 'Paolo', 'Diarmaid',
        'Bradlie', 'Ewen', 'Hussnan', 'Abdihakim', 'Nico', 'Gregory', 'Mayson', 'Lex', 'Zohaib', 'Khaleel',
        'Murrough', 'Dylin', 'Judah', 'Kieron', 'Eljay', 'Fionn', 'Tariq-Jay', 'Robby', 'Martin', 'Munmair',
        'Patrikas', 'Konar', 'Ryhs', 'Tyrnan', 'Sandy', 'Nilav', 'Ker', 'Cohan', 'Manas', 'Daren',
        'Lauren', 'Joeddy', 'Cator', 'Kohen', 'Keenan-Lee', 'Aaren', 'Jaxson', 'Aran', 'Owain', 'Bohbi',
        'Kriss', 'Kyden', 'Aiden-Jack', 'Cody-Lee', 'Johann', 'Darcy', 'Russell', 'Victor', 'Eshan', 'Riach',
        'Grzegorz', 'Eassan', 'Burhan', 'Caolan', 'Aleksandar', 'Koushik', 'Lukas', 'Jay', 'Connor-David', 'Patrick-John',
        'Alasdair', 'Nathanael', 'Kade', 'Keatin', 'Bailie', 'Ryleigh', 'Aman', 'Tayyib', 'Neco', 'Arved',
        'Troy', 'Corin', 'Howie', 'Amolpreet', 'Han', 'Deniss', 'Ammer', 'Jody', 'Rholmark', 'Corie',
        'Alexei', 'Bendeguz', 'Rohaan', 'Ryan', 'Jan', 'Connell', 'Josh', 'Adrien', 'Pawel', 'Bartlomiej',
        'Archie', 'Blessing', 'Matej', 'Zack', 'Marvin', 'Levi', 'Yves', 'Ruari', 'Ailin', 'Samir',
        'Jacob', 'Mitch', 'Lukmaan', 'Dagon', 'Reng', 'Thomas-Jay', 'Saifaddine', 'Davy', 'Syed', 'Guillaume',
        'Nathanial', 'Idris', 'Laurence', 'Zachariya', 'Zachery', 'Torin', 'Orrick', 'Clement', 'Harleigh', 'Raegan',
        'Drakeo', 'Mackenzie', 'Kinsey', 'Zamaar', 'Brody', 'Dara', 'Dawud', 'Andreas', 'Radmiras', 'Rajan',
        'Anthony', 'Taddy', 'Arturo', 'Tommi-Lee', 'Muhsin', 'Wayde', 'Andy', 'Fergal', 'Dawson', 'Malachi',
        'Taegan', 'Siddharth', 'Brajan', 'Aaryn', 'Bertie', 'Cody', 'Grahame', 'Ayyub', 'Shae', 'Lorne',
        'Denis', 'Walid', 'Yang', 'Dylan', 'Jarl', 'Cameron', 'Salter', 'Dustin', 'Zacharie', 'Mackie',
        'Tyler-Jay', 'Emlyn', 'Daniele', 'Graeme', 'Ari', 'Jonothan', 'Fintan', 'Loukas', 'Kaydyne', 'Abel',
        'Teighen', 'Kaydyn', 'Marco', 'Connor', 'Tye', 'Craig-James', 'Alihaider', 'Ainslie', 'Zaaine', 'Morris',
        'Ayub', 'Jedidiah', 'Bogdan', 'Mykie', 'Eason', 'Leighton', 'Macauley', 'Uzair', 'Taiwo', 'Easton',
        'Ayan', 'Rees', 'Fezaan', 'Odin', 'Tomasz', 'Ala', 'Deecan', 'Gavin-Lee', 'Aydon', 'Krzysiek',
        'Bowen', 'Greg', 'Linton', 'Cooper', 'Derron', 'Obieluem', 'Ryese', 'Harold', 'Abdur', 'Phinehas',
        'Eoin', 'Philip', 'Aiadan', 'Kingston', 'Tylar', 'Aazaan', 'Breandan', 'Andrea', 'Grady', 'Sultan',
        'Lancelot', 'Celik', 'Kerr', 'Daumantas', 'Vinh', 'Isak', 'Malakai', 'Caine', 'Derry', 'Haydn',
        'Deacon', 'Samuel', 'Rhudi', 'Alessandro', 'Harvinder', 'Kaleb', 'Ammaar', 'Taylor', 'Kainui', 'Tymoteusz',
        'Kadin', 'Atli', 'Lochlan', 'Tayler', 'Zen', 'Ceejay', 'Hashem', 'Blaine', 'Maaz', 'Tee',
        'Promise', 'Ojima-Ojo', 'Harvie', 'Zacharius', 'Jago', 'Geoff', 'Arafat', 'Keeman', 'Kristian', 'Macaully',
        'Shahmir', 'Olurotimi', 'Brendan', 'Kiya', 'Moad', 'Nawfal', 'Brendon', 'Findlay-James', 'Conor', 'Arunas',
        'Kaylem', 'Jaydan', 'Ranolph', 'Reiss', 'Carwyn', 'Cristian', 'Neshawn', 'Kaleem', 'Abdulkarem', 'Gurveer',
        'Oliver', 'Raunaq', 'Shadow', 'Jamal', 'Valentin', 'Edwin', 'Rahil', 'Tyllor', 'Roark', 'Cory',
        'Connal', 'Jaime', 'Caleb', 'Saim', 'Malachy', 'Carter', 'Rice', 'Caelan', 'Lawlyn', 'Ross-Andrew',
        'Dillan', 'Michat', 'Robbie-lee', 'Tyrone', 'David-Lee', 'Vincenzo', 'Macy', 'Lukasz', 'Dhani', 'Mathias',
        'Jenson', 'Abdur-Rahman', 'Malachai', 'Dermot', 'Meftah', 'Keigan', 'Kamran', 'Hamad', 'Isira', 'Harikrishna',
        'Layton', 'Colton', 'Jock', 'Sinai', 'Roan', 'Rheyden', 'Kier', 'Ahmed', 'Pardeepraj', 'Zachary',
        'Vinay', 'Kenzy', 'Bradly', 'Greig', 'Aaryan', 'Kenan', 'Zak', 'Shaughn', 'Devon', 'Brandan',
        'Rayden', 'Iagan', 'Warren', 'Timothy', 'Gian', 'Buddy', 'Kody', 'Nikash', 'Blaike', 'Ezekiel',
        'Kayleb', 'Keegan', 'Bradyn', 'Elliot', 'Adegbolahan', 'Sheriff', 'Devan', 'Xavier', 'Chiqal', 'Thorfinn',
        'Jackson', 'Jasper', 'Rasmus', 'Cobi', 'Raheem', 'Bony', 'Precious', 'Marty', 'Bryan', 'Roddy',
        'Quinn', 'Zohair', 'Garren', 'Shaarvin', 'Eamon', 'Digby', 'Yoji', 'Ilyaas', 'Marcquis', 'Tanner',
        'Braden', 'Ozzy', 'Jared', 'Badsha', 'Ameer', 'Rhein', 'Anselm', 'Humza', 'AJ', 'Denny',
        'Cairn', 'Rafi', 'Leigh', 'Abu', 'Ruo', 'Jorge', 'Toby', 'Kyie', 'Tyree', 'Dennys',
        'Giancarlo', 'Ashwin', 'Nikos', 'Jacques', 'Hosea', 'Jardine', 'Joey', 'Motade', 'Beau', 'Shay',
        'Lock', 'Conlly', 'Ricco', 'Pearsen', 'Peni', 'Jerome', 'Jordyn', 'McKenzie', 'Eric', 'Irvine',
        'Farhaan', 'Maxim', 'Zhuo', 'Amrit', 'Kyran', 'Rhyon', 'Lenin', 'Karam', 'Eli', 'Shergo',
        'Shawn', 'Callun', 'Cambell', 'Zerah', 'Conar', 'Aeron', 'Abdul-Rehman', 'Kearney', 'Sahil', 'Coll',
        'Derrin', 'Kurt', 'Dolan', 'Cassy', 'Orlando', 'Farren', 'Alyas', 'Cruz', 'Lyndsay', 'Alec',
        'Koden', 'Jian', 'Wesley', 'Bevin', 'Dyllan', 'John-Scott', 'Cobie', 'Moyes', 'Rafferty', 'Russel',
        'Carlos', 'Zidane', 'Rehan', 'Areeb', 'Kofi', 'Marzuq', 'Zuriel', 'Siergiej', 'Lennex', 'Michal',
        'Joss', 'Cael', 'Sahaib', 'Tanay', 'Titi', 'Zayn', 'Kinnon', 'Fikret', 'Alexx', 'Aodhan',
        'Jameil', 'Vinnie', 'Austen', 'Reece', 'Jordi', 'Luc', 'Aun', 'Olivier', 'Kenton', 'Graham',
        'Callan', 'Ezra', 'Ajay', 'Rowan', 'Diarmuid', 'Waqaas', 'Berkay', 'Kasper', 'Cayden-Tiamo', 'Zaki',
        'Gary', 'Kylan', 'Deshawn', 'Louis', 'Braydon', 'Kodie', 'Zainedin', 'Yanick', 'Callie', 'Wojciech',
        'Barney', 'Rhoridh', 'Orin', 'Kile', 'Casey', 'Aidian', 'Koray', 'Christian', 'Leigham', 'Zenith',
        'Ayren', 'Abdulkhader', 'Lennan', 'Aleksander', 'Marc-Anthony', 'Lachlan', 'Malakhy', 'Tane', 'Sean-Ray', 'Mason',
        'Shaurya', 'Moore', 'Boedyn', 'Coban', 'Andrei', 'Ricards', 'Rohit', 'Rayhan', 'Damien', 'Alistair',
        'Deegan', 'Mustafa', 'Baley', 'Kabir', 'Kamil', 'Blazey', 'Ruairidh', 'Koby', 'Cormack', 'Ashlee-jay',
        'Arryn', 'Ramsey', 'Kailin', 'Feden', 'Tony', 'Ritchie', 'Hugo', 'Loki', 'Johnpaul', 'Fynn',
        'Ridwan', 'Abdul', 'Kyaan', 'Danys', 'Daud', 'Nasser', 'Lael', 'Jamie', 'Dugald', 'Laird',
        'Jaksyn', 'Iestyn', 'Aristomenis', 'Dylan-John', 'Luis', 'Asrar', 'Adrian', 'Ivar', 'Hayden', 'Caie',
        'Benjamin', 'Colvin', 'Kealan', 'Abdul-Aziz', 'Emir', 'Ioannis', 'Elyan', 'Deelan', 'Rhuan', 'Nadeem',
        'Jamey', 'Bret', 'Conall', 'Limo', 'Reice', 'Finnean', 'Marko', 'Arun', 'Arran', 'Antonyo',
        'Raees', 'Favour', 'Malik', 'Kern', 'Del', 'Tayo', 'Subhaan', 'Alexander', 'Darl', 'Ege',
        'Kogan', 'Msughter', 'Jaydon', 'Oluwatobiloba', 'Crawford', 'Amani', 'Alieu', 'Eren', 'Etinosa', 'Lenny',
        'Bo', 'Craig', 'Sukhpal', 'Azedine', 'Alexzander', 'Farzad', 'Nathan', 'Jeffrey', 'Abdallah', 'Benn',
        'Mathu', 'Altyiab', 'Finn', 'Keelin', 'Moshy', 'Johndean', 'Tiernan', 'Umar', 'Kieren', 'Wilkie',
        'Anis', 'Ruslan', 'Nelson', 'Soham', 'McKauley', 'William', 'Caidan', 'Desmond', 'Avinash', 'Avi',
        'Ainsley', 'Remy', 'Cathal', 'Maneet', 'Macallum', 'Jasim', 'Kajetan', 'Reo', 'Reis', 'Shaw',
        'Ramsay', 'Glenn', 'Rivan', 'Clarke', 'Karsyn', 'Ossian', 'Zishan', 'Kenneth', 'Nicholas', 'Dex',
        'Mikey', 'Zaid', 'Lennen', 'Scott', 'Felix', 'Kenzi', 'Deon', 'Domenico', 'Averon', 'Adnan',
        'Kaine', 'Prentice', 'Ross', 'Dakota', 'Derin', 'Kylian', 'Mario', 'Connall', 'Omar', 'Arman',
        'Hussnain', 'Uzayr', 'Kieran-Scott', 'Damon', 'Phani', 'Alfy', 'Enrique', 'Axel', 'Robbie', 'A-Jay',
        'Doire', 'Samatar', 'Callin', 'Eljon', 'Rahman', 'Blair', 'Raymond', 'Corran', 'Youcef', 'Krish',
        'Leyland', 'Allister', 'Dissanayake', 'Leno', 'Taylor-Jay', 'Karl', 'Barrie', 'Arfin', 'Carrich', 'Byron',
        'Madison', 'Eddie', 'Xida', 'Devlyn', 'Kai', 'Krzysztof', 'Zarran', 'Kian', 'Saad', 'Dalton',
        'Killian', 'Thrinei', 'Bharath', 'Carrick', 'Kierin', 'Rubhan', 'Maros', 'Hassanali', 'Che', 'Zoubaeir',
        'Yannick', 'Maximus', 'Georgy', 'Aleem', 'Tanzeel', 'Ayomide', 'Odynn', 'Oryn', 'Guy', 'Charles',
        'Airlie', 'Briaddon', 'TJ', 'Tyler-James', 'Fletcher', 'Jayden-Paul', 'Colby', 'Hunter', 'Strachan', 'Harrison',
        'Jordan', 'Rubyn', 'Aristotelis', 'Joris', 'Adam', 'Ohran', 'Kaiwen', 'Kaywan', 'Derryn', 'Zakary',
        'Gursees', 'Aedin', 'Maksim', 'Geordan', 'Raphael', 'Khai', 'Keir', 'Lucais', 'Hussain', 'Arnav',
        'Haseeb', 'Colm', 'Leithen', 'Oran', 'Murray', 'James', 'Johnson', 'Jimmy', 'Anir', 'Abdullah',
        'Hassan', 'Asif', 'Reid', 'Aronas', 'Cavan', 'Lauchlin', 'Nevin', 'Rohan', 'Bracken', 'Flyn',
        'Shayaan', 'Anesu', 'CJ', 'Makensie', 'Rory', 'Sergei', 'Caedyn', 'Sonni', 'Firaaz', 'Artem',
        'Zain', 'Leylann', 'Aayan', 'Rafal', 'Trafford', 'Lasse', 'Kyren', 'Sullivan', 'Dean', 'Tyra',
        'Zacharias', 'Darn', 'Ryan-Lee', 'Phoenix', 'Corben', 'Ivan', 'Taegen', 'Shane', 'Macaulay', 'Montague',
        'Rufus', 'Dougray', 'Codi', 'Korey', 'Anubhav', 'Kellan', 'Bernard', 'Rihards', 'Ryo', 'Adain',
        'George', 'Santiago', 'Zion', 'Ace', 'Deagan', 'David', 'Hcen', 'Kairn', 'Satveer', 'Matthias',
        'Alum', 'Filippo', 'Chukwuemeka', 'Mitchell', 'Theodore', 'Jordy', 'Gurwinder', 'Calean', 'Aedyn', 'Babatunmise',
        'Jubin', 'Kaelum', 'Riyadh', 'Cadyn', 'Maddison', 'Keilan', 'Kean', 'Lee', 'Ericlee', 'Brandyn',
        'Jarell', 'Theo', 'Butchi', 'Alfie', 'Bryn', 'Pieter', 'Declyan', 'Jonny', 'Regan', 'Modoulamin',
        'Sohaib', 'Ferre', 'Rasul', 'Aon', 'Azlan', 'Ikechukwu', 'Marcel', 'Kealon', 'Tom', 'Myles',
        'Julien', 'Ryszard', 'Sean', 'Titus', 'Kenzeigh', 'Hussan', 'Landon', 'Lyle', 'Deryn', 'Kiaran',
        'Kallin', 'Eden', 'Eryk', 'Nyah', 'Mahan', 'Fodeba', 'McKade', 'Lenyn', 'Scot', 'Thiago',
        'Carlo', 'Erencem', 'Madaki', 'Eidhan', 'Martyn', 'Vincent', 'Ashley', 'Rishi', 'Bailey', 'Ravin',
        'Calley', 'Airen', 'Okeoghene', 'Branden', 'Harri', 'Hirvaansh', 'Reggie', 'Yadgor', 'Nickhill', 'Yahya',
        'Rui', 'Nicodemus', 'Yaseen', 'Kyral', 'Su', 'Curtis', 'Christopher-Lee', 'Cori', 'Tayye', 'Oswald',
        'Daithi', 'Ennis', 'Ziyaan', 'Lawrie', 'Harman', 'Griffyn', 'Majid', 'Corrie', 'Teagan', 'Tai',
        'Lachlann', 'Jayhan', 'Owais', 'Coray', 'Kedrick', 'Florin', 'Fred', 'Athon', 'Olaoluwapolorimi', 'Didier',
        'Lennox', 'Monty', 'Shaun-Paul', 'Ziya', 'Ewing', 'Robbi', 'Yann', 'Nicky', 'Reagan', 'Sanaullah',
        'Mylo', 'Angus', 'Ebow', 'Caileb-John', 'Tait', 'Kayam', 'Emerson', 'Annan', 'Ollie', 'Johnny'
        ]);
END;
$$;
