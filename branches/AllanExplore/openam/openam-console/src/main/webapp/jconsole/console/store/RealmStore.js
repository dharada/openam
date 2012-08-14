/**
 * Created with IntelliJ IDEA.
 * User: allan
 * Date: 8/11/12
 * Time: 15:02
 * To change this template use File | Settings | File Templates.
 */


Ext.define('AM.store.RealmStore', {
    extend: 'Ext.data.Store',

    model: 'AM.model.Service',
    autoLoad: true,
    storeName: 'realm',
    data: [
        {name:'GLOBAL'},
        {name:'TopLevel'}
    ],
    tobeproxy: {
        type: 'ajax',
        url: '../restconsole/orgs/',
        reader: {
            type: 'json',
            root: 'organization'
        }
    }
});
