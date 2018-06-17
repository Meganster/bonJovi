# Database API "Forum API"

# Описание
Реализация API для форума, работает с базой данных PostgreSQL. Язык написания: Java, используется фреймворк Spring. Есть возможность развернуть приложение при помощи Docker.

# Производительность
На данный момент разработанное API способно выдерживать 2151 запрос в секунду (request per second - rps), при разворачивании через docker-контейнер на машине со следующими характеристиками:
- RAM: 1GB
- CPU: Intel® Core™ i5-7400
- HDD: WDC WD10PURZ-85U  
    
Разворачивание сервера, т.е. сборка docker-контейнера происходит в пределах 3 минут.

# Документация
Задокументированное описание разработанного API можно посмотреть по ссылке:  
- [Backend API Swagger](https://app.swaggerhub.com/apis/Meganster/Database_API_Forum/1.0.0)

Контейнер можно собрать и запустить командами вида:
```
docker build -t y.van https://github.com/Meganster/bonJovi.git
docker run -p 5000:5000 --name y.van -t y.van
```
