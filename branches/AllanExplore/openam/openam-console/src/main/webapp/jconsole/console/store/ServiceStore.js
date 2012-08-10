/**
 * Created with IntelliJ IDEA.
 * User: allan
 * Date: 8/9/12
 * Time: 18:12
 * To change this template use File | Settings | File Templates.
 */


Ext.define('OpenAM.store.ServiceStore', {
    extend: 'Ext.data.Store',

    model: 'AMService',
    autoLoad: true,
    proxy: {
        type: 'ajax',
        url: '../restconsole/services/',
        reader: {
            type: 'json',
            root: 'serviceName'
        }
    }
});
