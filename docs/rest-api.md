# Rest API for simter-file

The rest context path `{context-path}` could be configured by property `module.rest-context-path.simter-file`.
Its default value is `/file`. The below URL is all relative to this context path.
For example a url `/x`, its really path should be `{context-path}/x`.

Provide rest APIs:

|    | Method | Url                          | Description
|----|--------|------------------------------|-------------
| 1  | GET    | /attachment/$id              | Get attachment form data
| 2  | GET    | /attachment                  | Pageable find attachment view data
| 3  | GET    | /parent/$puid/$upperId       | Find all module attachments data
| 4  | GET    | /inline/$id                  | Inline download one file
| 5  | GET    | /$id                         | Download one file
| 6  | POST   | /  (multipart/form-data)     | Upload one file through submit form way
| 7  | POST   | /  (application/octet-stream)| Upload one file through stream way
| 8  | DELETE | /$ids                        | Delete files

**Common query params:**

| Name     | Description
|----------|-----------
| id       | Identity
| page-no  | The page number to search, default 1
| page-size| The max return item count for each page, default 25

**{ATTACHMENT} data keys:**

| Name     | Description
|----------|-----------
| id       | The attachment identity
| name     | The folder name or file name without extension 
| type     | If it is a file, the value is file extension without dot symbol.<br/>And if it is a folder, the value is ":d".
| size     | The File or folder size
| path     | The path relative to upper folder that store the actual physical file.<br> If it's a root folder, the value is "".
| createOn | The creation time with ISO format `yyyy-MM-ddTHH:mm:ss.S+Z`
| creator  | The user that first create this attachment
| modifyOn | The last modified time
| modifier | The user that modified this attachment latest
| puid     | The belong business module identity
| upperId  | The upper identity
| fileName | The folder name or file name with extension

## 1. Get attachment form data

### Request

```
GET /attachment/$id
```

| Name     | Description
|----------|-----------
| id       | Identity

### Response (if found)

```
200 OK
Content-Type : application/json

{ATTACHMENT}
```

### Response (if not found)

```
404 Not Found
```

## 2. Pageable find attachment view data

### Request

```
GET /attachment?page-no=x&page-size=x
```

| Name    | Description
|---------|-----------
| page-no  | The page number to search, default 1
| page-size| The max return item count for each page, default 25

### Response

```
200 OK
Content-Type : application/json

{
  count, pageNo, pageSize,
  rows: [{ATTACHMENT}, ...]
}
```

`rows` sort by `createOn` field desc.

## 3. Find all module attachments data

### Request

```
GET /parent/$puid/$upperId
```

| Name    | Require | Description
|---------|---------|-----------
| puid    | true    | The business module identity
| upperId | false   | The upper id

### Response

```
200 OK
Content-Type : application/json

[{ATTACHMENT}, ...]
```

Sort by `createOn` field desc.

## 4. Inline download one file

### Request

```
GET /inline/$id
```

### Response (if found)

```
200 OK
Content-Type        : application/octet-stream
Content-Length      : $len
Content-Disposition : inline; filename="$name.$type"

$FILE_DATA
```

The `filename` value of the header `Content-Disposition` must be quoted by double quotation and decoded by ISO-8859-1 (RFC2183). The value of header `Content-Type` should prefer to use the actual MIME type of the real attachment file fallback to use `application/octet-stream` instead. MIME type reference from [here](https://www.iana.org/assignments/media-types/media-types.xhtml).

| Name        | Description
|-------------|-----------
| $len        | The file size with byte unit
| $name.$type | The file name with extension but without path

### Response (if not found)

```
404 Not Found
```

## 5. Download one file

### Request

```
GET /$id
```

### Response (if found)

```
200 OK
Content-Type        : application/octet-stream
Content-Length      : $len
Content-Disposition : attachment; filename="$name.$type"

$FILE_DATA
```

The `filename` value of the header `Content-Disposition` must be quoted by double quotation and decoded by ISO-8859-1 (RFC2183). The value of header `Content-Type` should prefer to use the actual MIME type of the real attachment file fallback to use `application/octet-stream` instead. MIME type reference from [here](https://www.iana.org/assignments/media-types/media-types.xhtml).

| Name        | Description
|-------------|-----------
| $len        | The file size with byte unit
| $name.$type | The file name with extension but without path

### Response (if not found)

```
404 Not Found
```

## 6. Upload one file through submit form way

### Request

Use a classic `'<input name="$inputName" type="file">'` way to submit a form.

```
POST /
Content-Type        : multipart/form-data; boundary=----$boundary
Content-Length      : $len

------$boundary
Content-Disposition: form-data; name="$inputName"; filename="$filename"
Content-Type: $mediaType

$FILE_DATA
------$boundary
Content-Disposition: form-data; name="puid"

$puid
------$boundary
Content-Disposition: form-data; name="upperId"

$upperId
------$boundary----
```

| Name       | Require | Description
|------------|---------|-----------
| $boundary  | true    | The random boundary value generated by client
| $len       | true    | The request body size with byte unit
| $inputName | true    | The input element name
| $filename  | true    | The file name with extension but without path
| $mediaType | true    | The file's media type
| $FILE_DATA | true    | The file's data
| $puid      | true    | The business module identity
| $upperId   | false   | The upper id

### Response

```
204 No Content
Location : /$id
```

## 7. Upload one file through stream way

### Request

Use `'Content-Type=application/octet-stream'` way to upload file. Mostly, this way is for upload file by ajax.

```
POST /?puid=x&upperId=x
Content-Type        : application/octet-stream
Content-Length      : $len
Content-Disposition : $filename

{file-data}
```

| Name       | Require | Description
|------------|---------|-----------
| $len       | true    | The request body size with byte unit
| $filename  | true    | The file name with extension but without path
| $puid      | false   | The business module identity
| $upperId   | false   | The upper id


### Response

```
204 No Content
Location : /$id
```

## 8. Delete files

### Request

```
Delete /$ids
```

| Name  | Description
|-------|-----------
| ids   | Combine multiple identity by comma symbol

### Response

```
204 No Content
```

If attachment not exists, ignore it as deleted successfully.