# Database API "Forum API"

# Описание
Реализация API для форума, работает с базой данных PostgreSQL. Язык написания: Java, используется фреймворк Spring. Есть возможность развернуть приложение при помощи Docker.

## Documentation
- [Backend API Swagger](https://app.swaggerhub.com/apis/Meganster/Database_API_Forum/1.0.0)

Контейнер можно собирать и запустить командами вида:
```
docker build -t y.van https://github.com/Meganster/bonJovi.git
docker run -p 5000:5000 --name y.van -t y.van
```
