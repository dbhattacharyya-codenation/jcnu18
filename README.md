# Assignment 2 for JCNU 2018

### Prerequisites

- docker
- docker-compose

### Build instructions

- Clone the repository
```
git clone https://github.com/dbhattacharyya-codenation/jcnu18.git
```
- `cd` into the directory
```
cd jcnu18
```
- Switch to `assignment2` branch
```
git checkout assignment2
```
- Run docker-compose up
```
docker-compose up
```

#### NOTE:
If you're using kerio-vpn, `docker-compose up` might not work locally.  
In that case, please disable kerio-vpn first using `/etc/init.d/kerio-kvc stop`.
### API endpoints

```
POST /fix

Request body:

{
    issueType : Integer,
    sandBoxURL : String (bolt URL of sandbox)
}

Response body;

{
    statusCode : Integer,
    statusMessage : String,
    fixId : Integer
}
```

```
GET /fix/{fixId}

Response Body:

{
    statusCode : Integer,
    statusMessage : String,
    fixId : Integer,
    s3Link : String,
    issueResponses : [
        {
            id : Integer,
            issuedTypeId : Integer,
            issueTypeDesc : Integer,
            fileName : String,
            lineNumber : Integer,
            columnNumber : Integer,
            isFixed : Boolean
        }
    ]
}
```

### Bugs

In case of any bugs, please feel free to raise an issue
